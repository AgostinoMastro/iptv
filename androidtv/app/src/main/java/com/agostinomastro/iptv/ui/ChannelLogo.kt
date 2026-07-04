package com.agostinomastro.iptv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.agostinomastro.iptv.model.Channel
import com.agostinomastro.iptv.model.favoriteKey
import com.agostinomastro.iptv.model.initials
import com.agostinomastro.iptv.model.logoUrls
import com.agostinomastro.iptv.ui.theme.PrimeColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelLogo(
    channel: Channel,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    showNameOnFailure: Boolean = false
) {
    val urls = remember(channel.favoriteKey) { channel.logoUrls() }
    var failedCount by remember(channel.favoriteKey) { mutableIntStateOf(0) }
    val url = urls.getOrNull(failedCount)

    if (url == null) {
        LogoFallback(channel = channel, showName = showNameOnFailure)
        return
    }

    key(failedCount) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = channel.name,
            modifier = modifier,
            contentScale = contentScale,
            loading = { LogoFallback(channel = channel, loading = true) },
            error = {
                LaunchedEffect(url) { failedCount++ }
                LogoFallback(channel = channel, showName = showNameOnFailure)
            },
            success = { SubcomposeAsyncImageContent() }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LogoFallback(
    channel: Channel,
    showName: Boolean = false,
    loading: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showName && !loading) {
            Text(
                text = channel.name,
                color = PrimeColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            Text(
                text = if (loading) "…" else channel.initials(),
                color = if (loading) PrimeColors.TextDisabled else PrimeColors.Accent,
                fontSize = if (loading) 28.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
