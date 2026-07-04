package com.agostinomastro.iptv.data

import android.content.Context

class FavoritesStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun isFavorite(key: String): Boolean = key in getAll()

    /** @return true if the channel was added, false if removed */
    fun toggle(key: String): Boolean {
        val updated = getAll().toMutableSet()
        return if (key in updated) {
            updated.remove(key)
            prefs.edit().putStringSet(KEY_FAVORITES, updated).apply()
            false
        } else {
            updated.add(key)
            prefs.edit().putStringSet(KEY_FAVORITES, updated).apply()
            true
        }
    }

    companion object {
        private const val PREFS_NAME = "iptv_favorites"
        private const val KEY_FAVORITES = "favorite_keys"
    }
}
