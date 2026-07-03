package com.aftab.musicplayer.data

import android.content.Context
import android.provider.MediaStore

object MediaStoreScanner {

    fun scan(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: ""
                val artist = cursor.getString(artistCol)
                songs.add(
                    Song(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(titleCol) ?: path.substringAfterLast('/'),
                        artist = if (artist == null || artist == MediaStore.UNKNOWN_STRING) "Unknown artist" else artist,
                        album = cursor.getString(albumCol) ?: "Unknown album",
                        albumId = cursor.getLong(albumIdCol),
                        duration = cursor.getLong(durationCol),
                        track = cursor.getInt(trackCol) % 1000,
                        dateAdded = cursor.getLong(dateCol),
                        folder = path.substringBeforeLast('/', "").substringAfterLast('/')
                    )
                )
            }
        }
        return songs
    }
}
