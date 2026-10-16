package com.vaultlab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vaultlab.app.data.SecureStore
import com.vaultlab.app.ui.HomeScreen
import com.vaultlab.app.ui.Screen
import com.vaultlab.app.ui.SettingsScreen
import com.vaultlab.app.ui.TempMailScreen
import com.vaultlab.app.ui.VaultLabTheme
import com.vaultlab.app.ui.VirusScanScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaultLabTheme {
                VaultLabApp()
            }
        }
    }
}

/** Tiny in-app navigation: one screen state, with the system back button returning home. */
@Composable
fun VaultLabApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val store = remember { SecureStore(context.applicationContext) }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    when (screen) {
        Screen.HOME -> HomeScreen(onOpen = { screen = it })
        Screen.TEMP_MAIL -> TempMailScreen(onBack = { screen = Screen.HOME })
        Screen.VIRUS_SCAN -> VirusScanScreen(
            store = store,
            onBack = { screen = Screen.HOME },
            onOpenSettings = { screen = Screen.SETTINGS },
        )
        Screen.SETTINGS -> SettingsScreen(store = store, onBack = { screen = Screen.HOME })
    }
}
