package com.agostinomastro.iptv.data

import android.content.Context
import com.agostinomastro.iptv.AppConfig
import com.agostinomastro.iptv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class PlaylistRepository(private val context: Context) {

    suspend fun loadChannels(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.filesDir, AppConfig.CACHE_FILE)

        val remote = runCatching { fetchRemote() }
        if (remote.isSuccess) {
            val content = remote.getOrThrow()
            cacheFile.writeText(content)
            return@withContext Result.success(M3uParser.parse(content))
        }

        if (cacheFile.exists()) {
            val cached = cacheFile.readText()
            if (cached.isNotBlank()) {
                return@withContext Result.success(M3uParser.parse(cached))
            }
        }

        Result.failure(remote.exceptionOrNull() ?: IllegalStateException("No playlist available"))
    }

    private fun fetchRemote(): String {
        val connection = (URL(AppConfig.PLAYLIST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "IPTV-FireTV/1.0")
        }

        return try {
            if (connection.responseCode !in 200..299) {
                error("HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
