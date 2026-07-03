package com.aftab.musicplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aftab.musicplayer.ui.theme.MusicTheme

private val audioPermission =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    if (audioGranted) {
        MainNav()
    } else {
        PermissionScreen(onGranted = { audioGranted = true })
    }
}

@Composable
private fun PermissionScreen(onGranted: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[audioPermission] == true) onGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Music needs access to the audio files on this device to build your library.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = {
                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(audioPermission, Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    arrayOf(audioPermission)
                }
                launcher.launch(perms)
            },
            modifier = Modifier.padding(top = 24.dp)
        ) { Text("Allow access") }
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            },
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Open app settings") }
    }
}

@Composable
private fun MainNav() {
    // Created here (activity scope) so every destination shares the same instances.
    val libraryVm: LibraryViewModel = viewModel()
    val playerVm: PlayerViewModel = viewModel()
    val playlistsVm: PlaylistsViewModel = viewModel()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                libraryVm = libraryVm,
                playerVm = playerVm,
                playlistsVm = playlistsVm,
                onOpenNowPlaying = { navController.navigate("nowplaying") },
                onOpenAlbum = { navController.navigate("album/$it") },
                onOpenArtist = { navController.navigate("artist/${Uri.encode(it)}") },
                onOpenPlaylist = { navController.navigate("playlist/$it") }
            )
        }
        composable("nowplaying") {
            NowPlayingScreen(
                playerVm = playerVm,
                onBack = { navController.popBackStack() },
                onOpenQueue = { navController.navigate("queue") }
            )
        }
        composable("queue") {
            QueueScreen(playerVm = playerVm, onBack = { navController.popBackStack() })
        }
        composable(
            "album/{albumId}",
            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
        ) { entry ->
            val albumId = entry.arguments!!.getLong("albumId")
            val album = libraryVm.albums.find { it.id == albumId }
            SongListScreen(
                title = album?.title ?: "Album",
                subtitle = album?.let { "${it.artist} · ${it.songs.size} songs" } ?: "",
                songs = album?.songs ?: emptyList(),
                playerVm = playerVm,
                playlistsVm = playlistsVm,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "artist/{name}",
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val name = entry.arguments!!.getString("name")!!
            val artist = libraryVm.artists.find { it.name == name }
            SongListScreen(
                title = artist?.name ?: "Artist",
                subtitle = artist?.let { "${it.songs.size} songs · ${it.albumCount} albums" } ?: "",
                songs = artist?.songs ?: emptyList(),
                playerVm = playerVm,
                playlistsVm = playlistsVm,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "playlist/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { entry ->
            val playlistId = entry.arguments!!.getLong("playlistId")
            val playlists by playlistsVm.playlists.collectAsState()
            val allTracks by playlistsVm.tracks.collectAsState()
            val playlist = playlists.find { it.id == playlistId }
            // Resolve stored media ids against the current library; skip files that no longer exist.
            val resolved = allTracks
                .filter { it.playlistId == playlistId }
                .sortedBy { it.position }
                .mapNotNull { track -> libraryVm.songById[track.mediaId]?.let { track to it } }
            SongListScreen(
                title = playlist?.name ?: "Playlist",
                subtitle = "${resolved.size} songs",
                songs = resolved.map { it.second },
                playerVm = playerVm,
                playlistsVm = playlistsVm,
                onBack = { navController.popBackStack() },
                onRemove = { index -> playlistsVm.removeTrack(resolved[index].first.id) }
            )
        }
    }
}
