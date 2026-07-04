package com.agostinomastro.iptv.ui

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.agostinomastro.iptv.PlayerActivity
import com.agostinomastro.iptv.data.FavoritesStore
import com.agostinomastro.iptv.data.PlaylistRepository
import com.agostinomastro.iptv.model.Channel
import com.agostinomastro.iptv.model.favoriteKey
import com.agostinomastro.iptv.ui.theme.PrimeColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel = viewModel(
        factory = BrowseViewModelFactory(
            PlaylistRepository(LocalContext.current.applicationContext),
            FavoritesStore(LocalContext.current.applicationContext)
        )
    )
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    fun toggleFavorite(channel: Channel) {
        val added = viewModel.toggleFavorite(channel)
        val message = if (added) "Added to Favourites" else "Removed from Favourites"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimeColors.Background)
    ) {
        when {
            state.isLoading -> LoadingView()
            state.error != null && state.groupedChannels.isEmpty() -> ErrorView(
                message = state.error ?: "Unknown error",
                onRetry = viewModel::refresh
            )
            else -> {
                val hero = state.displayedChannel
                val isHeroFavorite = hero?.favoriteKey in state.favoriteKeys

                fun onChannelClick(channel: Channel) {
                    if (viewModel.onChannelSelect(channel)) {
                        context.startActivity(PlayerActivity.intent(context, channel))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {
                    item {
                        HeroSection(
                            channel = hero,
                            isFavorite = isHeroFavorite,
                            onPlay = { channel ->
                                context.startActivity(PlayerActivity.intent(context, channel))
                            }
                        )
                    }

                    if (state.favoriteChannels.isNotEmpty()) {
                        item {
                            CategoryRow(
                                title = "Favourites",
                                channels = state.favoriteChannels,
                                favoriteKeys = state.favoriteKeys,
                                previewChannelKey = hero?.favoriteKey,
                                onChannelFocused = viewModel::previewChannel,
                                onChannelClick = ::onChannelClick,
                                onChannelLongClick = ::toggleFavorite
                            )
                        }
                    }

                    state.groupedChannels.forEach { (group, channels) ->
                        item {
                            CategoryRow(
                                title = group,
                                channels = channels,
                                favoriteKeys = state.favoriteKeys,
                                previewChannelKey = hero?.favoriteKey,
                                onChannelFocused = viewModel::previewChannel,
                                onChannelClick = ::onChannelClick,
                                onChannelLongClick = ::toggleFavorite
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroSection(
    channel: Channel?,
    isFavorite: Boolean,
    onPlay: (Channel) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        if (!channel?.logo.isNullOrBlank()) {
            AsyncImage(
                model = channel.logo,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.45f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PrimeColors.Surface, PrimeColors.Background)
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PrimeColors.Background,
                            PrimeColors.Background.copy(alpha = 0.92f),
                            PrimeColors.Background.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            PrimeColors.Background.copy(alpha = 0.4f),
                            PrimeColors.Background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 32.dp, end = 48.dp)
        ) {
            Text(
                text = "LIVE TV",
                color = PrimeColors.Accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = channel?.name ?: "Loading channels…",
                color = PrimeColors.TextPrimary,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (channel != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.group,
                        color = PrimeColors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (isFavorite) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "★ Favourite",
                            color = PrimeColors.Accent,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select to preview · Select again or press Play to watch · Hold Select to favourite",
                    color = PrimeColors.TextDisabled,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { onPlay(channel) }) {
                    Text("Play", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryRow(
    title: String,
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    previewChannelKey: String?,
    onChannelFocused: (Channel) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
            color = PrimeColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(channels, key = { "${it.group}-${it.name}-${it.url}" }) { channel ->
                ChannelCard(
                    channel = channel,
                    isFavorite = channel.favoriteKey in favoriteKeys,
                    isPreviewed = channel.favoriteKey == previewChannelKey,
                    onFocused = { onChannelFocused(channel) },
                    onClick = { onChannelClick(channel) },
                    onLongClick = { onChannelLongClick(channel) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    isPreviewed: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "cardScale")
    val shape = RoundedCornerShape(12.dp)

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(width = 180.dp, height = 120.dp)
            .scale(scale)
            .clip(shape)
            .background(if (isFocused) PrimeColors.CardFocused else PrimeColors.Card)
            .border(
                width = when {
                    isFocused -> 3.dp
                    isPreviewed -> 2.dp
                    else -> 1.dp
                },
                color = when {
                    isFocused -> PrimeColors.Accent
                    isPreviewed -> PrimeColors.AccentBright.copy(alpha = 0.7f)
                    else -> PrimeColors.Surface
                },
                shape = shape
            )
            .focusable(interactionSource = interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isFavorite) {
            Text(
                text = "★",
                color = PrimeColors.Accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
        if (!channel.logo.isNullOrBlank()) {
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = channel.name,
                color = PrimeColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Loading playlist…", color = PrimeColors.TextPrimary, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Fetching live channels", color = PrimeColors.TextSecondary)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Could not load playlist", color = PrimeColors.TextPrimary, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = PrimeColors.TextSecondary)
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
