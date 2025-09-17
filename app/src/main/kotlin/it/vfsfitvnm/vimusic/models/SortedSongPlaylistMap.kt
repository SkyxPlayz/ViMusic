package it.vfsfitvnm.vimusic.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@Immutable
@DatabaseView("SELECT * FROM SongPlaylistMap ORDER BY DESC")
data class SortedSongPlaylistMap(
    @ColumnInfo(index = true) val songId: Long,
    @ColumnInfo(index = true) val playlistId: Long,
    val position: Int
)
