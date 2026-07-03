package com.aftab.musicplayer.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aftab.musicplayer.data.Album
import com.aftab.musicplayer.data.Artist
import com.aftab.musicplayer.data.MediaStoreScanner
import com.aftab.musicplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortMode(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Recently added")
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var query by mutableStateOf("")
    var sortMode by mutableStateOf(SortMode.TITLE)

    val songById by derivedStateOf { songs.associateBy { it.id } }

    val filteredSongs by derivedStateOf {
        val q = query.trim()
        val base = if (q.isEmpty()) songs else songs.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
        when (sortMode) {
            SortMode.TITLE -> base.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> base.sortedWith(
                compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
            )
            SortMode.ALBUM -> base.sortedWith(
                compareBy({ it.album.lowercase() }, { it.track })
            )
            SortMode.DATE_ADDED -> base.sortedByDescending { it.dateAdded }
        }
    }

    val albums by derivedStateOf {
        val q = query.trim()
        songs.groupBy { it.albumId }
            .map { (id, albumSongs) ->
                Album(
                    id = id,
                    title = albumSongs.first().album,
                    artist = albumSongs.map { it.artist }.distinct().singleOrNull() ?: "Various artists",
                    songs = albumSongs.sortedBy { it.track }
                )
            }
            .filter {
                q.isEmpty() || it.title.contains(q, true) || it.artist.contains(q, true)
            }
            .sortedBy { it.title.lowercase() }
    }

    val artists by derivedStateOf {
        val q = query.trim()
        songs.groupBy { it.artist }
            .map { (name, artistSongs) -> Artist(name, artistSongs.sortedBy { it.title.lowercase() }) }
            .filter { q.isEmpty() || it.name.contains(q, true) }
            .sortedBy { it.name.lowercase() }
    }

    init {
        // The permission gate in MainActivity only shows the library UI once
        // audio permission is granted, so a scan here can safely query MediaStore.
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                MediaStoreScanner.scan(getApplication())
            }
            songs = result
            isLoading = false
        }
    }
}
