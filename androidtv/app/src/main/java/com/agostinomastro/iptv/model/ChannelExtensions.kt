package com.agostinomastro.iptv.model

/** Stable key for persisting favourites (stream URL is unique per channel). */
val Channel.favoriteKey: String
    get() = url
