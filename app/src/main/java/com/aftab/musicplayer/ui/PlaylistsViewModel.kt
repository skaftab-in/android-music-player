package com.aftab.musicplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aftab.musicplayer.MusicApp
import com.aftab.musicplayer.data.Playlist
import com.aftab.musicplayer.data.PlaylistTrack
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = (app as MusicApp).database.dao()

    val playlists = dao.playlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All playlist tracks, ordered by playlist then position. */
    val tracks = dao.allTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createPlaylist(name: String, songIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val id = dao.insertPlaylist(Playlist(name = name.trim()))
            if (songIds.isNotEmpty()) {
                dao.insertTracks(songIds.mapIndexed { i, songId ->
                    PlaylistTrack(playlistId = id, mediaId = songId, position = i)
                })
            }
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { dao.renamePlaylist(id, name.trim()) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { dao.deletePlaylist(id) }
    }

    fun addToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            val start = dao.maxPosition(playlistId) + 1
            dao.insertTracks(songIds.mapIndexed { i, songId ->
                PlaylistTrack(playlistId = playlistId, mediaId = songId, position = start + i)
            })
        }
    }

    fun removeTrack(trackRowId: Long) {
        viewModelScope.launch { dao.deleteTrack(trackRowId) }
    }
}
