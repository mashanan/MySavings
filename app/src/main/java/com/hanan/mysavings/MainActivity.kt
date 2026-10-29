package com.hanan.mysavings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.hanan.mysavings.ui.navigation.MySavingsApp
import com.hanan.mysavings.ui.settings.AppCompatLanguageController
import com.hanan.mysavings.ui.theme.MySavingsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Single-activity app: every screen is a composable destination inside MySavingsApp's NavHost.
// AppCompatActivity (not ComponentActivity) so the in-app language choice also works on Android 10-12.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var languageController: AppCompatLanguageController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        languageController.applyDefaultOnFirstLaunch()
        enableEdgeToEdge()
        setContent {
            MySavingsTheme {
                MySavingsApp()
            }
        }
    }
}
