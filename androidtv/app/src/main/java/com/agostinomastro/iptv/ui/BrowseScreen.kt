package com.agostinomastro.iptv.ui

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.tv.material3.Button
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.agostinomastro.iptv.PlayerActivity
import com.agostinomastro.iptv.data.PlaylistRepository
import com.agostinomastro.iptv.model.Channel
import com.agostinomastro.iptv.ui.theme.PrimeColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel = viewModel(
        factory = BrowseViewModelFactory(
            PlaylistRepository(LocalContext.current.applicationContext)
        )
    )
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

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
                val hero = state.focusedChannel ?: state.heroChannel
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {
                    item {
                        HeroSection(
                            channel = hero,
                            onPlay = { channel ->
                                context.startActivity(PlayerActivity.intent(context, channel))
                            }
                        )
                    }

                    state.groupedChannels.forEach { (group, channels) ->
                        item {
                            CategoryRow(
                                title = group,
                                channels = channels,
                                onChannelFocused = viewModel::onChannelFocused,
                                onChannelClick = { channel ->
                                    context.startActivity(PlayerActivity.intent(context, channel))
                                }
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
    onPlay: (Channel) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimeColors.Surface,
                        PrimeColors.Background
                    )
                )
            )
    ) {
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
                Text(
                    text = channel.group,
                    color = PrimeColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
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
    onChannelFocused: (Channel) -> Unit,
    onChannelClick: (Channel) -> Unit
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
                    onFocused = { onChannelFocused(channel) },
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelCard(
    channel: Channel,
    onFocused: () -> Unit,
    onClick: () -> Unit
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
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) PrimeColors.Accent else PrimeColors.Surface,
                shape = shape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            },
        contentAlignment = Alignment.Center
    ) {
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
