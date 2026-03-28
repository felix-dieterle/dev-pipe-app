package com.devpipe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.devpipe.app.data.storage.PreferencesManager
import com.devpipe.app.ui.navigation.NavGraph
import com.devpipe.app.ui.theme.DevPipeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by preferencesManager.theme.collectAsState(initial = "system")
            val darkTheme = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            DevPipeTheme(darkTheme = darkTheme) {
                NavGraph()
            }
        }
    }
}
