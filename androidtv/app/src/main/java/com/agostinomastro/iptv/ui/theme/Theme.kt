package com.agostinomastro.iptv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val PrimeDarkScheme = darkColorScheme(
    primary = PrimeColors.Accent,
    onPrimary = Color.White,
    secondary = PrimeColors.Surface,
    onSecondary = PrimeColors.TextPrimary,
    background = PrimeColors.Background,
    onBackground = PrimeColors.TextPrimary,
    surface = PrimeColors.Card,
    onSurface = PrimeColors.TextPrimary,
    surfaceVariant = PrimeColors.Surface,
    onSurfaceVariant = PrimeColors.TextSecondary
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PrimeDarkScheme,
        content = content
    )
}
