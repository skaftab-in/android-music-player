package com.aftab.musicplayer.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistTrack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val mediaId: Long,
    val position: Int
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    fun playlists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId, position")
    fun allTracks(): Flow<List<PlaylistTrack>>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert
    suspend fun insertTracks(tracks: List<PlaylistTrack>)

    @Query("DELETE FROM playlist_tracks WHERE id = :trackRowId")
    suspend fun deleteTrack(trackRowId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int
}

@Database(entities = [Playlist::class, PlaylistTrack::class], version = 1, exportSchema = false)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun dao(): PlaylistDao

    companion object {
        fun build(context: Context): PlaylistDatabase =
            Room.databaseBuilder(context, PlaylistDatabase::class.java, "playlists.db").build()
    }
}
