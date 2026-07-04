package com.agostinomastro.iptv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agostinomastro.iptv.data.FavoritesStore
import com.agostinomastro.iptv.data.PlaylistRepository

class BrowseViewModelFactory(
    private val repository: PlaylistRepository,
    private val favoritesStore: FavoritesStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowseViewModel::class.java)) {
            return BrowseViewModel(repository, favoritesStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
