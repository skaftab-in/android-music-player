package com.aftab.musicplayer

import android.app.Application
import com.aftab.musicplayer.data.PlaylistDatabase

class MusicApp : Application() {
    val database: PlaylistDatabase by lazy { PlaylistDatabase.build(this) }
}
