package com.aftab.musicplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aftab.musicplayer.data.Song

/**
 * Shared list-of-songs screen used by album, artist, and playlist details.
 * [onRemove] (playlist only) removes the song at that list index from the playlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    playerVm: PlayerViewModel,
    playlistsVm: PlaylistsViewModel,
    onBack: () -> Unit,
    onRemove: ((Int) -> Unit)? = null
) {
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    val playlists by playlistsVm.playlists.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (songs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No songs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { playerVm.playSongs(songs) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("Play", modifier = Modifier.padding(start = 4.dp))
                    }
                    FilledTonalButton(
                        onClick = { playerVm.shuffleSongs(songs) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Text("Shuffle", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            items(songs.size) { index ->
                val song = songs[index]
                SongRow(
                    song = song,
                    isCurrent = playerVm.currentMediaId == song.id,
                    onClick = { playerVm.playSongs(songs, index) },
                    onPlayNext = { playerVm.playNext(song) },
                    onAddToQueue = { playerVm.addToQueue(song) },
                    onAddToPlaylist = { songForPlaylist = song },
                    extraMenuLabel = if (onRemove != null) "Remove from playlist" else null,
                    onExtraMenuClick = if (onRemove != null) {
                        { onRemove(index) }
                    } else null
                )
            }
        }
    }

    songForPlaylist?.let { song ->
        AddToPlaylistDialog(
            playlists = playlists,
            onPick = { playlistsVm.addToPlaylist(it.id, listOf(song.id)) },
            onCreateNew = { name -> playlistsVm.createPlaylist(name, listOf(song.id)) },
            onDismiss = { songForPlaylist = null }
        )
    }
}
