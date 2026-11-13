package com.opencode.mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.opencode.mobile.ui.ChatScreen
import com.opencode.mobile.ui.OpenCodeTheme
import com.opencode.mobile.ui.SessionListScreen
import com.opencode.mobile.ui.SettingsScreen
import kotlinx.coroutines.flow.MutableStateFlow

private sealed interface Route {
    data object Sessions : Route
    data object Settings : Route
    data class Chat(val session: SessionItem) : Route
}

/** Deep link target coming from a notification tap. */
object Navigation {
    val openSession = MutableStateFlow<String?>(null)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            OpenCodeTheme {
                Surface(Modifier.fillMaxSize()) { App() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("session_id")?.let { Navigation.openSession.value = it }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("opencode", Context.MODE_PRIVATE) }

    var server by remember { mutableStateOf(prefs.getString("server", "").orEmpty()) }
    var password by remember { mutableStateOf(prefs.getString("password", "").orEmpty()) }
    var route by remember { mutableStateOf<Route>(Route.Sessions) }
    var watching by remember { mutableStateOf(prefs.getBoolean("watch", false)) }

    val api = remember(server, password) {
        if (server.isBlank()) null else Api(server, password)
    }

    fun startWatching() {
        prefs.edit().putBoolean("watch", true).apply()
        watching = true
        ContextCompat.startForegroundService(
            context,
            Intent(context, SessionWatcherService::class.java)
        )
    }

    fun stopWatching() {
        prefs.edit().putBoolean("watch", false).apply()
        watching = false
        context.stopService(Intent(context, SessionWatcherService::class.java))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startWatching()
        } else {
            watching = false
            prefs.edit().putBoolean("watch", false).apply()
        }
    }

    // Resume watching after a cold start.
    LaunchedEffect(Unit) {
        if (watching) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, SessionWatcherService::class.java)
                )
            }
        }
    }

    // Notification tap → open that session.
    LaunchedEffect(Unit) {
        Navigation.openSession.collect { id ->
            if (id != null) {
                if (api != null) {
                    route = Route.Chat(
                        SessionItem(
                            id = id,
                            title = "Session",
                            updated = System.currentTimeMillis(),
                            created = System.currentTimeMillis(),
                            cost = 0.0
                        )
                    )
                }
                Navigation.openSession.value = null
            }
        }
    }

    val showSettings = api == null || route is Route.Settings

    BackHandler(enabled = showSettings && api != null) { route = Route.Sessions }
    BackHandler(enabled = api != null && route is Route.Chat) { route = Route.Sessions }

    Crossfade(
        targetState = if (showSettings) "settings" else route,
        animationSpec = tween(220),
        label = "screen"
    ) { target ->
        when (target) {
            is Route.Chat -> if (api != null) {
                ChatScreen(
                    api = api,
                    session = target.session,
                    onBack = { route = Route.Sessions },
                    onSettings = { route = Route.Settings }
                )
            }

            else -> if (api == null) {
                SettingsScreen(
                    initialServer = server,
                    initialPassword = password,
                    canCancel = false,
                    watching = false,
                    onWatchingChange = {},
                    onCancel = {},
                    onSave = { s, p ->
                        server = s.trim()
                        password = p
                        prefs.edit().putString("server", server).putString("password", password).apply()
                        route = Route.Sessions
                    }
                )
            } else if (target == "settings") {
                SettingsScreen(
                    initialServer = server,
                    initialPassword = password,
                    canCancel = true,
                    watching = watching,
                    onWatchingChange = { enabled ->
                        if (!enabled) {
                            stopWatching()
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            startWatching()
                        }
                    },
                    onCancel = { route = Route.Sessions },
                    onSave = { s, p ->
                        server = s.trim()
                        password = p
                        prefs.edit().putString("server", server).putString("password", password).apply()
                        route = Route.Sessions
                    }
                )
            } else {
                SessionListScreen(
                    api = api,
                    onOpen = { route = Route.Chat(it) },
                    onSettings = { route = Route.Settings },
                    onCreate = {
                        runCatching { api.createSession() }.getOrNull()?.let { id ->
                            SessionItem(
                                id = id,
                                title = "New session",
                                updated = System.currentTimeMillis(),
                                created = System.currentTimeMillis(),
                                cost = 0.0
                            )
                        }
                    }
                )
            }
        }
    }
}
