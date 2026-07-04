package com.agostinomastro.iptv.model

data class Channel(
    val name: String,
    val url: String,
    val logo: String?,
    val group: String,
    val tvgId: String?
)
