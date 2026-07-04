package com.agostinomastro.iptv.model

import java.util.Locale

object GroupTitles {
    private const val OTHER_RAW = "Undefined"
    const val OTHER_LABEL = "Other"

    fun displayTitle(rawGroup: String): String {
        val group = rawGroup.trim().ifBlank { "General" }
        if (group.equals(OTHER_RAW, ignoreCase = true)) return OTHER_LABEL

        return group.split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = " · ") { formatSegment(it) }
    }

    /** Sort alphabetically, but keep Undefined/Other last. */
    fun sortKey(rawGroup: String): String {
        val group = rawGroup.trim().ifBlank { "General" }
        if (group.equals(OTHER_RAW, ignoreCase = true)) return "zzz_$OTHER_LABEL"
        return displayTitle(group).lowercase()
    }

    private fun formatSegment(segment: String): String =
        segment.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
}
