package com.hanan.strukku

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. [HiltAndroidApp] generates the app-level DI container
 * that every other Hilt component (activities, ViewModels, workers) hangs off.
 */
@HiltAndroidApp
class StrukKuApp : Application()
