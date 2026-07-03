package com.aftab.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.aftab.musicplayer.data.Song
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerVm: PlayerViewModel,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val item = playerVm.currentItem
    if (item == null) {
        // Nothing loaded (e.g. process restarted on this screen) — nothing to show here.
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var position by remember { mutableLongStateOf(playerVm.position()) }
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playerVm.isPlaying, item) {
        while (true) {
            if (!dragging) position = playerVm.position()
            delay(250)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now playing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Artwork(
                    item.mediaMetadata.artworkUri,
                    size = 360.dp,
                    cornerRadius = 16.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                item.mediaMetadata.title?.toString() ?: "Unknown",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.mediaMetadata.artist?.toString() ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(24.dp))

            val duration = playerVm.duration
            Slider(
                value = if (dragging) dragPosition else position.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(0f)),
                onValueChange = {
                    dragging = true
                    dragPosition = it
                },
                onValueChangeFinished = {
                    playerVm.seekTo(dragPosition.toLong())
                    position = dragPosition.toLong()
                    dragging = false
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatDuration(if (dragging) dragPosition.toLong() else position),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(formatDuration(duration), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerVm.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerVm.shuffleEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { playerVm.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(40.dp)
                    )
                }
                FilledIconButton(
                    onClick = { playerVm.togglePlayPause() },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        if (playerVm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerVm.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { playerVm.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { playerVm.cycleRepeatMode() }) {
                    Icon(
                        if (playerVm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne
                        else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (playerVm.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(playerVm: PlayerViewModel, onBack: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (playerVm.currentIndex in playerVm.queue.indices) {
            listState.scrollToItem(playerVm.currentIndex)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queue (${playerVm.queue.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.padding(padding)) {
            itemsIndexed(playerVm.queue) { index, mediaItem ->
                val isCurrent = index == playerVm.currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { playerVm.playQueueItem(index) }
                        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(mediaItem.mediaMetadata.artworkUri, size = 44.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            mediaItem.mediaMetadata.artist?.toString() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { playerVm.removeQueueItem(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove from queue")
                    }
                }
            }
        }
    }
}
