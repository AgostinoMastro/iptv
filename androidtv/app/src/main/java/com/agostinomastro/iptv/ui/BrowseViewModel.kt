package com.agostinomastro.iptv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agostinomastro.iptv.data.FavoritesStore
import com.agostinomastro.iptv.data.PlaylistRepository
import com.agostinomastro.iptv.model.Channel
import com.agostinomastro.iptv.model.favoriteKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val allChannels: List<Channel> = emptyList(),
    val groupedChannels: Map<String, List<Channel>> = emptyMap(),
    val favoriteKeys: Set<String> = emptySet(),
    val heroChannel: Channel? = null,
    val focusedChannel: Channel? = null,
    val fromCache: Boolean = false
) {
    val favoriteChannels: List<Channel>
        get() = allChannels.filter { it.favoriteKey in favoriteKeys }
}

class BrowseViewModel(
    private val repository: PlaylistRepository,
    private val favoritesStore: FavoritesStore
) : ViewModel() {

    private val _state = MutableStateFlow(
        BrowseUiState(favoriteKeys = favoritesStore.getAll())
    )
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.loadChannels()
                .onSuccess { channels ->
                    val grouped = channels
                        .groupBy { it.group.ifBlank { "General" } }
                        .toSortedMap(compareBy { it.lowercase() })

                    val favorites = favoritesStore.getAll()
                    val hero = channels.firstOrNull()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            allChannels = channels,
                            groupedChannels = grouped,
                            favoriteKeys = favorites,
                            heroChannel = hero,
                            focusedChannel = hero,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load playlist"
                        )
                    }
                }
        }
    }

    fun onChannelFocused(channel: Channel) {
        _state.update { it.copy(focusedChannel = channel) }
    }

    /** @return true if added, false if removed */
    fun toggleFavorite(channel: Channel): Boolean {
        val added = favoritesStore.toggle(channel.favoriteKey)
        _state.update { it.copy(favoriteKeys = favoritesStore.getAll()) }
        return added
    }

    fun isFavorite(channel: Channel): Boolean =
        channel.favoriteKey in _state.value.favoriteKeys
}
