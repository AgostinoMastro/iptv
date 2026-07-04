package com.agostinomastro.iptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.agostinomastro.iptv.data.RecentsStore
import com.agostinomastro.iptv.model.Channel

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()

        val channel = intent.getChannelExtra()
            ?: run {
                finish()
                return
            }

        RecentsStore(this).add(channel)

        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.player_view)
        configureControls()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 90_000, 2_500, 5_000)
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()
            .also { exoPlayer ->
                playerView.player = exoPlayer
                playerView.subtitleView?.visibility = View.GONE

                exoPlayer.trackSelectionParameters = TrackSelectionParameters.Builder(this)
                    .setDisabledTrackTypes(setOf(C.TRACK_TYPE_TEXT, C.TRACK_TYPE_METADATA))
                    .build()

                exoPlayer.setMediaItem(
                    MediaItem.Builder()
                        .setUri(channel.url)
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(3_000)
                                .build()
                        )
                        .build()
                )
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        playerView.showController()
                    }

                    override fun onEvents(player: Player, events: Player.Events) {
                        if (events.containsAny(
                                Player.EVENT_TIMELINE_CHANGED,
                                Player.EVENT_MEDIA_ITEM_TRANSITION,
                                Player.EVENT_TRACKS_CHANGED
                            )
                        ) {
                            updateSeekControls()
                        }
                    }
                })
            }

        title = channel.name
    }

    override fun onStop() {
        player?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val exoPlayer = player ?: return super.onKeyDown(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                togglePlayPause(exoPlayer)
                playerView.showController()
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (!playerView.isControllerFullyVisible) {
                    playerView.showController()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                if (seekIfAllowed(exoPlayer, -SEEK_INCREMENT_MS)) {
                    playerView.showController()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                if (atLiveEdge(exoPlayer)) {
                    super.onKeyDown(keyCode, event)
                } else if (seekIfAllowed(exoPlayer, SEEK_INCREMENT_MS)) {
                    playerView.showController()
                    true
                } else {
                    jumpToLive(exoPlayer)
                    playerView.showController()
                    true
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun configureControls() {
        playerView.setShowRewindButton(true)
        playerView.setShowFastForwardButton(true)
        playerView.setShowPreviousButton(false)
        playerView.setShowNextButton(false)
        playerView.setShowSubtitleButton(false)
        playerView.setShowVrButton(false)
        playerView.controllerShowTimeoutMs = 5_000
        playerView.controllerHideOnTouch = false

        playerView.post {
            listOf(
                androidx.media3.ui.R.id.exo_prev,
                androidx.media3.ui.R.id.exo_next,
                androidx.media3.ui.R.id.exo_settings,
                androidx.media3.ui.R.id.exo_progress,
                androidx.media3.ui.R.id.exo_duration,
                androidx.media3.ui.R.id.exo_position,
                androidx.media3.ui.R.id.exo_time,
                androidx.media3.ui.R.id.exo_subtitle
            ).forEach { id ->
                playerView.findViewById<View>(id)?.visibility = View.GONE
            }
        }
    }

    private fun updateSeekControls() {
        val exoPlayer = player ?: return
        val seekable = exoPlayer.isCurrentMediaItemSeekable
        playerView.setShowRewindButton(seekable)
        playerView.setShowFastForwardButton(seekable)
    }

    private fun atLiveEdge(exoPlayer: Player): Boolean {
        if (!exoPlayer.isCurrentMediaItemLive) return false
        val offsetMs = exoPlayer.currentLiveOffset
        if (offsetMs == C.TIME_UNSET) return exoPlayer.isPlaying
        return offsetMs <= LIVE_EDGE_THRESHOLD_MS
    }

    private fun togglePlayPause(exoPlayer: Player) {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    private fun seekIfAllowed(exoPlayer: Player, deltaMs: Long): Boolean {
        if (!exoPlayer.isCurrentMediaItemSeekable) return false
        if (deltaMs < 0) exoPlayer.seekBack() else exoPlayer.seekForward()
        return true
    }

    private fun jumpToLive(exoPlayer: Player) {
        if (exoPlayer.isCurrentMediaItemLive) {
            exoPlayer.seekToDefaultPosition()
        } else {
            exoPlayer.seekTo(exoPlayer.duration.coerceAtLeast(0L))
        }
        exoPlayer.play()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val SEEK_INCREMENT_MS = 10_000L
        private const val LIVE_EDGE_THRESHOLD_MS = 5_000L

        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_GROUP = "extra_group"
        private const val EXTRA_LOGO = "extra_logo"
        private const val EXTRA_TVG_ID = "extra_tvg_id"

        fun intent(context: Context, channel: Channel): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_NAME, channel.name)
                putExtra(EXTRA_URL, channel.url)
                putExtra(EXTRA_GROUP, channel.group)
                putExtra(EXTRA_LOGO, channel.logo)
                putExtra(EXTRA_TVG_ID, channel.tvgId)
            }
        }

        private fun Intent.getChannelExtra(): Channel? {
            val url = getStringExtra(EXTRA_URL) ?: return null
            val name = getStringExtra(EXTRA_NAME) ?: "Channel"
            val group = getStringExtra(EXTRA_GROUP) ?: "General"
            val logo = getStringExtra(EXTRA_LOGO)
            val tvgId = getStringExtra(EXTRA_TVG_ID)
            return Channel(name = name, url = url, logo = logo, group = group, tvgId = tvgId)
        }
    }
}
