package com.hanan.mysavings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hanan.mysavings.ui.navigation.MySavingsNavHost
import com.hanan.mysavings.ui.theme.MySavingsTheme
import dagger.hilt.android.AndroidEntryPoint

// Single-activity app: every screen is a composable destination inside MySavingsNavHost.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MySavingsTheme {
                MySavingsNavHost()
            }
        }
    }
}
