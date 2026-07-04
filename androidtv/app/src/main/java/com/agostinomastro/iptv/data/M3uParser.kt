package com.agostinomastro.iptv.data

import com.agostinomastro.iptv.model.Channel

object M3uParser {
    private val attrRegex = Regex("""(\w[\w-]*)="([^"]*)"""")
    private val nameSuffixRegex = Regex(""",\s*(.+)$""")

    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var index = 0

        while (index < lines.size) {
            val line = lines[index].trim()
            if (!line.startsWith("#EXTINF", ignoreCase = true)) {
                index++
                continue
            }

            val attrs = mutableMapOf<String, String>()
            attrRegex.findAll(line).forEach { match ->
                attrs[match.groupValues[1].lowercase()] = match.groupValues[2]
            }

            val name = nameSuffixRegex.find(line)?.groupValues?.get(1)?.trim()
                ?: attrs["tvg-name"]
                ?: "Unknown"

            index++
            while (index < lines.size) {
                val urlLine = lines[index].trim()
                index++
                if (urlLine.isEmpty() || urlLine.startsWith("#")) {
                    if (urlLine.startsWith("#EXTINF", ignoreCase = true)) {
                        index--
                    }
                    continue
                }

                channels += Channel(
                    name = name,
                    url = urlLine,
                    logo = attrs["tvg-logo"],
                    group = attrs["group-title"]?.takeIf { it.isNotBlank() } ?: "General",
                    tvgId = attrs["tvg-id"]
                )
                break
            }
        }

        return channels
    }
}
