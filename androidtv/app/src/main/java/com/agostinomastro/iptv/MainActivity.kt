package com.agostinomastro.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.agostinomastro.iptv.ui.BrowseScreen
import com.agostinomastro.iptv.ui.theme.IptvTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IptvTheme {
                BrowseScreen()
            }
        }
    }
}
