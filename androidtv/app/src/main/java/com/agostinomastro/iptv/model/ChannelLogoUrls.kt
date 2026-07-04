package com.agostinomastro.iptv.model

fun Channel.logoUrls(): List<String> {
    val primary = logo?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
    val urls = mutableListOf(primary)
    wikimediaPngThumb(primary)?.let { thumb ->
        if (thumb != primary) urls.add(thumb)
    }
    return urls
}

private fun wikimediaPngThumb(url: String): String? {
    if (!url.contains("upload.wikimedia.org", ignoreCase = true)) return null
    if (!url.endsWith(".svg", ignoreCase = true)) return null

    val path = url.substringAfter("upload.wikimedia.org/").trimStart('/')
    val segments = path.split('/')
    if (segments.size < 4 || segments[0] != "wikipedia") return null

    val filename = segments.last()
    val prefix = segments.dropLast(1).joinToString("/")
    return "https://upload.wikimedia.org/$prefix/thumb/$filename/320px-$filename.png"
}

fun Channel.initials(): String {
    val words = name.split(Regex("""\s+""")).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.isNotEmpty() -> words[0].take(2).uppercase()
        else -> "TV"
    }
}
