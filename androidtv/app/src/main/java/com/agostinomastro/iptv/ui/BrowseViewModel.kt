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
    val previewChannel: Channel? = null,
    val searchQuery: String = "",
    val fromCache: Boolean = false
) {
    val favoriteChannels: List<Channel>
        get() = allChannels.filter { it.favoriteKey in favoriteKeys }

    val displayedChannel: Channel?
        get() = previewChannel ?: heroChannel

    val isSearching: Boolean
        get() = searchQuery.isNotBlank()

    val searchResults: List<Channel>
        get() = if (!isSearching) emptyList() else allChannels.filter { it.matchesSearch(searchQuery) }

    val visibleGroupedChannels: Map<String, List<Channel>>
        get() {
            if (!isSearching) return groupedChannels
            return searchResults
                .groupBy { it.group.ifBlank { "General" } }
                .toSortedMap(compareBy { it.lowercase() })
        }
}

private fun Channel.matchesSearch(query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return name.lowercase().contains(q) ||
        group.lowercase().contains(q) ||
        (tvgId?.lowercase()?.contains(q) == true)
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
                            previewChannel = hero,
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

    /** Updates the hero preview when navigating with D-pad focus. */
    fun previewChannel(channel: Channel) {
        _state.update { it.copy(previewChannel = channel) }
    }

    /**
     * Card select/click: first action previews in the hero; second action on the
     * same channel starts playback. Returns true when playback should begin.
     */
    fun onChannelSelect(channel: Channel): Boolean {
        val current = _state.value.displayedChannel
        return if (current?.favoriteKey == channel.favoriteKey) {
            true
        } else {
            _state.update { it.copy(previewChannel = channel) }
            false
        }
    }

    /** @return true if added, false if removed */
    fun toggleFavorite(channel: Channel): Boolean {
        val added = favoritesStore.toggle(channel.favoriteKey)
        _state.update { it.copy(favoriteKeys = favoritesStore.getAll()) }
        return added
    }

    fun isFavorite(channel: Channel): Boolean =
        channel.favoriteKey in _state.value.favoriteKeys

    fun setSearchQuery(query: String) {
        _state.update { state ->
            val results = if (query.isBlank()) emptyList() else state.allChannels.filter { it.matchesSearch(query) }
            val preview = when {
                query.isNotBlank() && results.isNotEmpty() -> results.first()
                query.isBlank() -> state.previewChannel ?: state.heroChannel
                else -> state.previewChannel
            }
            state.copy(searchQuery = query, previewChannel = preview)
        }
    }

    fun clearSearch() {
        setSearchQuery("")
    }
}
