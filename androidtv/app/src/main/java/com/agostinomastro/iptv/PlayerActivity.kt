package com.agostinomastro.iptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.agostinomastro.iptv.model.Channel

class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()

        val channel = intent.getChannelExtra()
            ?: run {
                finish()
                return
            }

        val playerView = PlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            useController = true
            controllerShowTimeoutMs = 4000
            controllerHideOnTouch = false
        }

        setContentView(
            FrameLayout(this).apply {
                addView(playerView)
            }
        )

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            exoPlayer.setMediaItem(MediaItem.fromUri(channel.url))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    // Keep controller visible so the user can back out.
                }
            })
        }

        title = channel.name
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
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
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_GROUP = "extra_group"

        fun intent(context: Context, channel: Channel): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_NAME, channel.name)
                putExtra(EXTRA_URL, channel.url)
                putExtra(EXTRA_GROUP, channel.group)
            }
        }

        private fun Intent.getChannelExtra(): Channel? {
            val url = getStringExtra(EXTRA_URL) ?: return null
            val name = getStringExtra(EXTRA_NAME) ?: "Channel"
            val group = getStringExtra(EXTRA_GROUP) ?: "General"
            return Channel(name = name, url = url, logo = null, group = group, tvgId = null)
        }
    }
}
