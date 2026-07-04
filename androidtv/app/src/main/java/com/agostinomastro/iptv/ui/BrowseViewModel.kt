package com.agostinomastro.iptv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agostinomastro.iptv.data.PlaylistRepository
import com.agostinomastro.iptv.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val groupedChannels: Map<String, List<Channel>> = emptyMap(),
    val heroChannel: Channel? = null,
    val focusedChannel: Channel? = null,
    val fromCache: Boolean = false
)

class BrowseViewModel(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
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

                    val hero = channels.firstOrNull()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            groupedChannels = grouped,
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
}
