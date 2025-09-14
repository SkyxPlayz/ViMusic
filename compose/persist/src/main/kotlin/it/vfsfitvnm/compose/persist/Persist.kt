package com.vimusic.data  // <-- GANTI dengan package app kamu + .data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.Executors

data class FavoriteSong(
    val songId: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

object FavoriteManager {
    private const val PREFS = "vimusic_prefs"
    private const val KEY_FAVORITES = "favorites_json"
    private val gson = Gson()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun loadListSync(ctx: Context): MutableList<FavoriteSong> {
        val json = prefs(ctx).getString(KEY_FAVORITES, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<FavoriteSong>>() {}.type
            gson.fromJson<MutableList<FavoriteSong>>(json, type) ?: mutableListOf()
        } catch (t: Throwable) {
            mutableListOf()
        }
    }

    private fun saveListSync(ctx: Context, list: List<FavoriteSong>) {
        prefs(ctx).edit().putString(KEY_FAVORITES, gson.toJson(list)).apply()
    }

    fun isFavoriteAsync(ctx: Context, songId: String, cb: (Boolean) -> Unit) {
        executor.execute {
            val res = loadListSync(ctx).any { it.songId == songId }
            mainHandler.post { cb(res) }
        }
    }

    fun getFavoritesAsync(ctx: Context, cb: (List<FavoriteSong>) -> Unit) {
        executor.execute {
            val res = loadListSync(ctx).sortedByDescending { it.addedAt }
            mainHandler.post { cb(res) }
        }
    }

    fun addFavoriteAsync(ctx: Context, songId: String, title: String? = null, artist: String? = null, album: String? = null, cb: (() -> Unit)? = null) {
        executor.execute {
            val list = loadListSync(ctx)
            list.removeAll { it.songId == songId }
            list.add(0, FavoriteSong(songId, title, artist, album))
            saveListSync(ctx, list)
            cb?.let { mainHandler.post(it) }
        }
    }

    fun removeFavoriteAsync(ctx: Context, songId: String, cb: (() -> Unit)? = null) {
        executor.execute {
            val list = loadListSync(ctx)
            val removed = list.removeAll { it.songId == songId }
            if (removed) saveListSync(ctx, list)
            cb?.let { mainHandler.post(it) }
        }
    }

    fun toggleFavoriteAsync(ctx: Context, songId: String, title: String? = null, artist: String? = null, album: String? = null, cb: ((isNowFavorite: Boolean) -> Unit)? = null) {
        executor.execute {
            val list = loadListSync(ctx)
            val exists = list.any { it.songId == songId }
            if (exists) {
                list.removeAll { it.songId == songId }
            } else {
                list.add(0, FavoriteSong(songId, title, artist, album))
            }
            saveListSync(ctx, list)
            mainHandler.post { cb?.invoke(!exists) }
        }
    }
}
