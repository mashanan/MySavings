package com.hanan.strukku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hanan.strukku.ui.navigation.StrukKuNavHost
import com.hanan.strukku.ui.theme.StrukKuTheme
import dagger.hilt.android.AndroidEntryPoint

// Single-activity app: every screen is a composable destination inside StrukKuNavHost.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrukKuTheme {
                StrukKuNavHost()
            }
        }
    }
}
