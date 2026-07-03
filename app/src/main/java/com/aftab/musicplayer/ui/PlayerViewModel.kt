package com.aftab.musicplayer.ui

import android.app.Application
import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aftab.musicplayer.data.Song
import com.aftab.musicplayer.data.toMediaItem
import com.aftab.musicplayer.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    var currentItem by mutableStateOf<MediaItem?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var duration by mutableLongStateOf(0L)
        private set
    var shuffleEnabled by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set
    var queue by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var currentIndex by mutableIntStateOf(0)
        private set

    val hasMedia: Boolean get() = currentItem != null
    val currentMediaId: Long? get() = currentItem?.mediaId?.toLongOrNull()

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentItem = mediaItem
            currentIndex = controller?.currentMediaItemIndex ?: 0
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }

        override fun onPlaybackStateChanged(state: Int) {
            duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
        }

        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            shuffleEnabled = enabled
        }

        override fun onRepeatModeChanged(mode: Int) {
            repeatMode = mode
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            rebuildQueue()
        }
    }

    init {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(listener)
            // Sync state in case the service was already playing when the UI reconnected.
            currentItem = c.currentMediaItem
            isPlaying = c.isPlaying
            duration = c.duration.coerceAtLeast(0L)
            shuffleEnabled = c.shuffleModeEnabled
            repeatMode = c.repeatMode
            currentIndex = c.currentMediaItemIndex
            rebuildQueue()
        }, MoreExecutors.directExecutor())
    }

    private fun rebuildQueue() {
        val c = controller ?: return
        queue = (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
        currentIndex = c.currentMediaItemIndex
    }

    fun position(): Long = controller?.currentPosition ?: 0L

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val c = controller ?: return
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun shuffleSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        playSongs(songs.shuffled(), 0)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            playSongs(listOf(song))
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
        }
    }

    fun addToQueue(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            playSongs(listOf(song))
        } else {
            c.addMediaItem(song.toMediaItem())
        }
    }

    fun playQueueItem(index: Int) {
        val c = controller ?: return
        c.seekTo(index, 0L)
        c.play()
    }

    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}
