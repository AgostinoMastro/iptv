package com.agostinomastro.iptv.data

import android.content.Context
import com.agostinomastro.iptv.model.Channel

class RecentsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun add(channel: Channel) {
        val updated = getEntries()
            .filter { it.url != channel.url }
            .toMutableList()
        updated.add(0, RecentEntry.from(channel))
        save(updated.take(MAX_RECENTS))
    }

    fun getEntries(): List<RecentEntry> {
        val raw = prefs.getString(KEY_RECENTS, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .mapNotNull { line -> RecentEntry.decode(line) }
            .toList()
    }

    fun resolveChannels(allChannels: List<Channel>): List<Channel> {
        val byUrl = allChannels.associateBy { it.url }
        return getEntries().map { entry -> byUrl[entry.url] ?: entry.toChannel() }
    }

    private fun save(entries: List<RecentEntry>) {
        prefs.edit()
            .putString(KEY_RECENTS, entries.joinToString("\n") { it.encode() })
            .apply()
    }

    data class RecentEntry(
        val url: String,
        val name: String,
        val group: String,
        val logo: String?,
        val tvgId: String?
    ) {
        fun toChannel() = Channel(name = name, url = url, logo = logo, group = group, tvgId = tvgId)

        fun encode(): String = listOf(
            url,
            name.replace('\t', ' '),
            group.replace('\t', ' '),
            logo.orEmpty(),
            tvgId.orEmpty()
        ).joinToString("\t")

        companion object {
            fun from(channel: Channel) = RecentEntry(
                url = channel.url,
                name = channel.name,
                group = channel.group,
                logo = channel.logo,
                tvgId = channel.tvgId
            )

            fun decode(line: String): RecentEntry? {
                val parts = line.split('\t')
                if (parts.size < 3) return null
                return RecentEntry(
                    url = parts[0],
                    name = parts[1],
                    group = parts[2],
                    logo = parts.getOrNull(3)?.takeIf { it.isNotBlank() },
                    tvgId = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
                )
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "iptv_recents"
        private const val KEY_RECENTS = "recent_entries"
        private const val MAX_RECENTS = 25
    }
}
