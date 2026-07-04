package com.agostinomastro.iptv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agostinomastro.iptv.data.FavoritesStore
import com.agostinomastro.iptv.data.PlaylistRepository
import com.agostinomastro.iptv.data.RecentsStore
import com.agostinomastro.iptv.model.Channel
import com.agostinomastro.iptv.model.GroupTitles
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
    val recentChannels: List<Channel> = emptyList(),
    val heroChannel: Channel? = null,
    val previewChannel: Channel? = null,
    val activeFilter: BrowseFilter = BrowseFilter.Home,
    val searchExpanded: Boolean = false,
    val searchQuery: String = "",
    val fromCache: Boolean = false
) {
    val favoriteChannels: List<Channel>
        get() = allChannels.filter { it.favoriteKey in favoriteKeys }

    val displayedChannel: Channel?
        get() = previewChannel ?: heroChannel

    val isSearching: Boolean
        get() = searchExpanded && searchQuery.isNotBlank()

    val filterBaseChannels: List<Channel>
        get() = channelsForFilter(activeFilter, allChannels, favoriteKeys, recentChannels)

    val searchResults: List<Channel>
        get() = if (!isSearching) emptyList() else filterBaseChannels.filter { it.matchesSearch(searchQuery) }

    val contentSections: List<ContentSection>
        get() {
            if (isSearching) {
                return if (searchResults.isEmpty()) emptyList()
                else listOf(ContentSection("Search results (${searchResults.size})", searchResults))
            }

            return when (activeFilter) {
                BrowseFilter.Home -> buildList {
                    if (favoriteChannels.isNotEmpty()) {
                        add(ContentSection("Favourites", favoriteChannels))
                    }
                    if (recentChannels.isNotEmpty()) {
                        add(ContentSection("Recents", recentChannels))
                    }
                    groupedChannels.forEach { (group, channels) ->
                        add(ContentSection(GroupTitles.displayTitle(group), channels))
                    }
                }
                BrowseFilter.Favourites -> {
                    if (favoriteChannels.isEmpty()) emptyList()
                    else listOf(ContentSection("Favourites", favoriteChannels))
                }
                BrowseFilter.Recents -> {
                    if (recentChannels.isEmpty()) emptyList()
                    else listOf(ContentSection("Recents", recentChannels))
                }
                else -> {
                    val channels = filterBaseChannels
                    if (channels.isEmpty()) emptyList()
                    else listOf(ContentSection(activeFilter.label, channels))
                }
            }
        }
}

data class ContentSection(
    val title: String,
    val channels: List<Channel>
)

private fun Channel.matchesSearch(query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return name.lowercase().contains(q) ||
        group.lowercase().contains(q) ||
        (tvgId?.lowercase()?.contains(q) == true)
}

private fun Channel.matchesCategory(category: String): Boolean {
    val cat = category.lowercase()
    return group.split(';').any { segment ->
        val s = segment.trim().lowercase()
        s == cat || s.contains(cat)
    }
}

private fun channelsForFilter(
    filter: BrowseFilter,
    allChannels: List<Channel>,
    favoriteKeys: Set<String>,
    recentChannels: List<Channel>
): List<Channel> = when (filter) {
    BrowseFilter.Home -> allChannels
    BrowseFilter.Favourites -> allChannels.filter { it.favoriteKey in favoriteKeys }
    BrowseFilter.Recents -> recentChannels
    BrowseFilter.Canada -> allChannels.filter { it.matchesCategory("Canada") }
    BrowseFilter.News -> allChannels.filter { it.matchesCategory("News") }
    BrowseFilter.Sports -> allChannels.filter { it.matchesCategory("Sports") }
    BrowseFilter.Movies -> allChannels.filter { it.matchesCategory("Movies") }
}

class BrowseViewModel(
    private val repository: PlaylistRepository,
    private val favoritesStore: FavoritesStore,
    private val recentsStore: RecentsStore
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
                        .toSortedMap(compareBy { GroupTitles.sortKey(it) })

                    val favorites = favoritesStore.getAll()
                    val recents = recentsStore.resolveChannels(channels)
                    val hero = channels.firstOrNull()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            allChannels = channels,
                            groupedChannels = grouped,
                            favoriteKeys = favorites,
                            recentChannels = recents,
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

    fun refreshRecents() {
        _state.update { state ->
            state.copy(recentChannels = recentsStore.resolveChannels(state.allChannels))
        }
    }

    fun previewChannel(channel: Channel) {
        _state.update { it.copy(previewChannel = channel) }
    }

    fun toggleFavorite(channel: Channel): Boolean {
        val added = favoritesStore.toggle(channel.favoriteKey)
        _state.update { it.copy(favoriteKeys = favoritesStore.getAll()) }
        return added
    }

    fun setActiveFilter(filter: BrowseFilter) {
        _state.update { state ->
            val base = channelsForFilter(filter, state.allChannels, state.favoriteKeys, state.recentChannels)
            state.copy(
                activeFilter = filter,
                searchExpanded = false,
                searchQuery = "",
                previewChannel = base.firstOrNull() ?: state.heroChannel
            )
        }
    }

    fun toggleSearch() {
        _state.update { state ->
            val expanded = !state.searchExpanded
            state.copy(
                searchExpanded = expanded,
                searchQuery = if (expanded) state.searchQuery else ""
            )
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { state ->
            val pool = state.filterBaseChannels
            val results = if (query.isBlank()) emptyList() else pool.filter { it.matchesSearch(query) }
            val preview = when {
                query.isNotBlank() && results.isNotEmpty() -> results.first()
                query.isBlank() -> pool.firstOrNull() ?: state.heroChannel
                else -> state.previewChannel
            }
            state.copy(searchQuery = query, previewChannel = preview)
        }
    }

    fun clearSearch() {
        _state.update { state ->
            state.copy(
                searchQuery = "",
                previewChannel = state.filterBaseChannels.firstOrNull() ?: state.heroChannel
            )
        }
    }
}
