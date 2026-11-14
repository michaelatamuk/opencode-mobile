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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.opencode.mobile.ui.ChatScreen
import com.opencode.mobile.ui.OpenCodeTheme
import com.opencode.mobile.ui.SessionListScreen
import com.opencode.mobile.ui.SettingsScreen
import com.opencode.mobile.ui.SharePickerSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Deep link target coming from a notification tap. */
object Navigation {
    val openSession = MutableStateFlow<String?>(null)
}

data class SharePayload(val text: String?, val uri: android.net.Uri?)

/** Content shared into the app from another application. */
object ShareTarget {
    val payload = MutableStateFlow<SharePayload?>(null)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        Shortcuts.sync(this)
        UiPrefs.load(this)
        setContent {
            val mode by UiPrefs.mode.collectAsState()
            val accent by UiPrefs.accent.collectAsState()
            OpenCodeTheme(mode = mode, accent = accent) {
                Surface(Modifier.fillMaxSize()) { Root() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        intent.getStringExtra("session_id")?.let { Navigation.openSession.value = it }

        if (intent.getBooleanExtra("new_session", false)) {
            Navigation.openSession.value = "new"
        }

        if (intent.action == Intent.ACTION_VIEW && intent.data?.scheme == "opencode") {
            intent.data?.pathSegments?.firstOrNull()?.let { Navigation.openSession.value = it }
        }

        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            @Suppress("DEPRECATION")
            val uri: android.net.Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (text != null || uri != null) {
                ShareTarget.payload.value = SharePayload(text, uri)
            }
        }
    }
}

@Composable
private fun Root() {
    val context = LocalContext.current
    val prefs = remember { SecurePrefs.get(context) }

    var server by remember { mutableStateOf(prefs.getString("server", "").orEmpty()) }
    var password by remember { mutableStateOf(prefs.getString("password", "").orEmpty()) }
    var openSessionID by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var watching by remember { mutableStateOf(prefs.getBoolean("watch", false)) }
    var watchdogMinutes by remember { mutableStateOf(prefs.getInt("watchdog_minutes", 0)) }
    var persistentWatch by remember { mutableStateOf(prefs.getBoolean("watch_persistent", false)) }
    var share by remember { mutableStateOf<SharePayload?>(null) }

    val repo = remember(server, password) {
        if (server.isBlank()) null else OpenCodeRepository(Api(server, password))
    }
    val scope = AppScope

    fun startWatcher() {
        val intent = Intent(context, SessionWatcherService::class.java)
        if (persistentWatch) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun startWatching() {
        prefs.edit().putBoolean("watch", true).apply()
        watching = true
        startWatcher()
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

    LaunchedEffect(Unit) {
        if (watching) {
            runCatching { startWatcher() }
        }
    }

    LaunchedEffect(Unit) {
        Navigation.openSession.collect { id ->
            if (id != null) {
                if (repo != null) {
                    if (id == "new") {
                        openSessionID = null
                        draft = true
                    } else {
                        draft = false
                        openSessionID = id
                    }
                }
                Navigation.openSession.value = null
            }
        }
    }

    LaunchedEffect(Unit) {
        ShareTarget.payload.collect { payload -> if (payload != null) share = payload }
    }

    val showSettings = repo == null || settingsOpen
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

    BackHandler(enabled = showSettings && repo != null) { settingsOpen = false }
    BackHandler(enabled = repo != null && (openSessionID != null || draft) && !showSettings) {
        openSessionID = null
        draft = false
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp && repo != null

        when {
            repo == null -> SettingsScreen(
                initialServer = server,
                initialPassword = password,
                canCancel = false,
                watching = false,
                notificationsEnabled = notificationsEnabled,
                watchdogMinutes = watchdogMinutes,
                persistentWatch = persistentWatch,
                onWatchingChange = {},
                onWatchdogChange = {},
                onPersistentWatchChange = {},
                onTestNotification = { Notify.test(context) },
                onCancel = {},
                onSave = { s, p ->
                    server = s.trim()
                    password = p
                    prefs.edit().putString("server", server).putString("password", password).apply()
                }
            )

            showSettings -> SettingsScreen(
                initialServer = server,
                initialPassword = password,
                canCancel = true,
                watching = watching,
                notificationsEnabled = notificationsEnabled,
                watchdogMinutes = watchdogMinutes,
                persistentWatch = persistentWatch,
                onWatchdogChange = { minutes ->
                    watchdogMinutes = minutes
                    prefs.edit().putInt("watchdog_minutes", minutes).apply()
                },
                onPersistentWatchChange = { enabled ->
                    persistentWatch = enabled
                    prefs.edit().putBoolean("watch_persistent", enabled).apply()
                    if (watching) {
                        // Restart so the service picks up the new mode.
                        context.stopService(Intent(context, SessionWatcherService::class.java))
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            startWatcher()
                        }, 250)
                    }
                },
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
                onTestNotification = { Notify.test(context) },
                onCancel = { settingsOpen = false },
                onSave = { s, p ->
                    server = s.trim()
                    password = p
                    prefs.edit().putString("server", server).putString("password", password).apply()
                    settingsOpen = false
                }
            )

            wide -> Row(Modifier.fillMaxSize()) {
                SessionListScreen(
                    repo = repo,
                    onOpen = { openSessionID = it.id; draft = false },
                    onSettings = { settingsOpen = true },
                    onNewSession = { openSessionID = null; draft = true },
                    modifier = Modifier.weight(0.42f)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (openSessionID != null || draft) {
                    ChatScreen(
                        repo = repo,
                        sessionID = if (draft) null else openSessionID,
                        showBack = false,
                        onBack = { openSessionID = null; draft = false },
                        onSessionCreated = { id -> draft = false; openSessionID = id },
                        modifier = Modifier.weight(0.58f)
                    )
                } else {
                    Box(
                        Modifier.weight(0.58f).fillMaxSize().background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.chat_pick_session),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> Crossfade(
                targetState = if (draft) "draft" else openSessionID,
                animationSpec = tween(220),
                label = "screen"
            ) { key ->
                if (key == null) {
                    SessionListScreen(
                        repo = repo,
                        onOpen = { openSessionID = it.id; draft = false },
                        onSettings = { settingsOpen = true },
                        onNewSession = { openSessionID = null; draft = true }
                    )
                } else {
                    ChatScreen(
                        repo = repo,
                        sessionID = if (key == "draft") null else key.toString(),
                        showBack = true,
                        onBack = { openSessionID = null; draft = false },
                        onSessionCreated = { id -> draft = false; openSessionID = id }
                    )
                }
            }
        }
    }

    if (share != null && repo != null) {
        SharePickerSheet(
            api = repo.api,
            hasImage = share?.uri != null,
            onDismiss = {
                share = null
                ShareTarget.payload.value = null
            },
            onPick = { session ->
                val payload = share
                share = null
                ShareTarget.payload.value = null
                scope.launch {
                    try {
                        val attachment = payload?.uri?.let { uri ->
                            withContext(Dispatchers.IO) { AttachmentLoader.load(context, uri) }
                        }
                        val text = payload?.text?.takeIf { it.isNotBlank() }
                            ?: if (attachment != null) "Shared image." else "Shared."
                        repo.send(session.id, text, listOfNotNull(attachment))
                        draft = false
                        openSessionID = session.id
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            friendlyError(e),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}
