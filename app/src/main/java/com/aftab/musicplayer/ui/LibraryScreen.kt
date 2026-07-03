package com.aftab.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aftab.musicplayer.data.Playlist
import com.aftab.musicplayer.data.Song

private val TAB_TITLES = listOf("Songs", "Albums", "Artists", "Playlists")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel,
    playlistsVm: PlaylistsViewModel,
    onOpenNowPlaying: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    val playlists by playlistsVm.playlists.collectAsState()

    Scaffold(
        topBar = {
            if (searchActive) {
                TextField(
                    value = libraryVm.query,
                    onValueChange = { libraryVm.query = it },
                    placeholder = { Text("Search songs, albums, artists") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            libraryVm.query = ""
                            searchActive = false
                        }) { Icon(Icons.Default.Close, contentDescription = "Close search") }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                TopAppBar(
                    title = { Text("Music") },
                    actions = {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        if (selectedTab == 0) {
                            SortMenuButton(libraryVm)
                        }
                        IconButton(onClick = { libraryVm.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rescan library")
                        }
                    }
                )
            }
        },
        bottomBar = { MiniPlayer(playerVm, onOpen = onOpenNowPlaying) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                TAB_TITLES.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            if (libraryVm.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> SongsTab(libraryVm, playerVm, onAddToPlaylist = { songForPlaylist = it })
                    1 -> AlbumsTab(libraryVm, onOpenAlbum)
                    2 -> ArtistsTab(libraryVm, onOpenArtist)
                    3 -> PlaylistsTab(playlistsVm, onOpenPlaylist)
                }
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

@Composable
private fun SortMenuButton(libraryVm: LibraryViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    leadingIcon = {
                        RadioButton(selected = libraryVm.sortMode == mode, onClick = null)
                    },
                    onClick = {
                        libraryVm.sortMode = mode
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SongsTab(
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel,
    onAddToPlaylist: (Song) -> Unit
) {
    val songs = libraryVm.filteredSongs
    if (songs.isEmpty()) {
        EmptyState("No songs found")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
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
                    Text("Play all", modifier = Modifier.padding(start = 4.dp))
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
        items(songs, key = { it.id }) { song ->
            SongRow(
                song = song,
                isCurrent = playerVm.currentMediaId == song.id,
                onClick = { playerVm.playSongs(songs, songs.indexOf(song)) },
                onPlayNext = { playerVm.playNext(song) },
                onAddToQueue = { playerVm.addToQueue(song) },
                onAddToPlaylist = { onAddToPlaylist(song) }
            )
        }
    }
}

@Composable
private fun AlbumsTab(libraryVm: LibraryViewModel, onOpenAlbum: (Long) -> Unit) {
    val albums = libraryVm.albums
    if (albums.isEmpty()) {
        EmptyState("No albums found")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                modifier = Modifier.clickable { onOpenAlbum(album.id) }
            ) {
                Artwork(album.artworkUri, size = 150.dp, cornerRadius = 12.dp)
                Text(
                    album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(libraryVm: LibraryViewModel, onOpenArtist: (String) -> Unit) {
    val artists = libraryVm.artists
    if (artists.isEmpty()) {
        EmptyState("No artists found")
        return
    }
    LazyColumn {
        items(artists, key = { it.name }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenArtist(artist.name) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(artist.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${artist.songs.size} songs · ${artist.albumCount} albums",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(playlistsVm: PlaylistsViewModel, onOpenPlaylist: (Long) -> Unit) {
    val playlists by playlistsVm.playlists.collectAsState()
    val tracks by playlistsVm.tracks.collectAsState()
    val counts = remember(tracks) { tracks.groupingBy { it.playlistId }.eachCount() }

    var creating by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }

    LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { creating = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "New playlist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
        items(playlists, key = { it.id }) { playlist ->
            var menuOpen by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(playlist.id) }
                    .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${counts[playlist.id] ?: 0} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Playlist options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { menuOpen = false; renaming = playlist }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { menuOpen = false; playlistsVm.deletePlaylist(playlist.id) }
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        TextInputDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { playlistsVm.createPlaylist(it); creating = false },
            onDismiss = { creating = false }
        )
    }
    renaming?.let { playlist ->
        TextInputDialog(
            title = "Rename playlist",
            initialValue = playlist.name,
            confirmLabel = "Rename",
            onConfirm = { playlistsVm.renamePlaylist(playlist.id, it); renaming = null },
            onDismiss = { renaming = null }
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
