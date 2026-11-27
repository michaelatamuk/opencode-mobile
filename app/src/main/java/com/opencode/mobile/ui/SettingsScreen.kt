package com.opencode.mobile.ui

import android.content.Intent
import android.content.Context
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.opencode.mobile.Api
import com.opencode.mobile.ApiException
import com.opencode.mobile.AgentOption
import com.opencode.mobile.AppScope
import com.opencode.mobile.Attachment
import com.opencode.mobile.IntegrationItem
import com.opencode.mobile.McpServer
import com.opencode.mobile.McpResource
import com.opencode.mobile.ModelOption
import com.opencode.mobile.OpenCodeWidgetProvider
import com.opencode.mobile.Pairing
import com.opencode.mobile.PendingStore
import com.opencode.mobile.ProfileStore
import com.opencode.mobile.R
import com.opencode.mobile.SecurePrefs
import com.opencode.mobile.ServerProfile
import com.opencode.mobile.UiPrefs
import com.opencode.mobile.UsageStore
import kotlinx.coroutines.launch

private enum class SettingsPage { Home, Connection, Profiles, Notifications, Appearance, Automation, Pending, Connections, Voice, Widget, Security, About }

private enum class ConnState { Unknown, Testing, Ok, Failed }

private data class AccentChoice(val id: String, val color: Color, val label: Int)

private val ACCENTS = listOf(
    AccentChoice("blue", Color(0xFF8FB4FF), R.string.settings_accent_blue),
    AccentChoice("mint", Color(0xFF9FE7C3), R.string.settings_accent_mint),
    AccentChoice("violet", Color(0xFFE7C6FF), R.string.settings_accent_violet),
    AccentChoice("amber", Color(0xFFFFD59B), R.string.settings_accent_amber)
)

@Composable
fun SettingsScreen(
    initialServer: String,
    initialPassword: String,
    initialFallback: String,
    canCancel: Boolean,
    watching: Boolean,
    notificationsEnabled: Boolean,
    watchdogMinutes: Int,
    persistentWatch: Boolean,
    onWatchingChange: (Boolean) -> Unit,
    onWatchdogChange: (Int) -> Unit,
    onPersistentWatchChange: (Boolean) -> Unit,
    onTestNotification: () -> Unit,
    onCancel: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var server by remember { mutableStateOf(initialServer.ifBlank { "http://192.168.1.10:28894" }) }
    var password by remember { mutableStateOf(initialPassword) }
    var fallback by remember { mutableStateOf(initialFallback) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    var pairInput by remember { mutableStateOf("") }
    var pairError by remember { mutableStateOf<String?>(null) }
    var pairing by remember { mutableStateOf(false) }

    var budget by remember {
        mutableStateOf(SecurePrefs.get(context).getString("budget_usd", "").orEmpty())
    }
    var monthBudget by remember {
        mutableStateOf(SecurePrefs.get(context).getString("budget_month_usd", "").orEmpty())
    }
    var biometricLock by remember {
        mutableStateOf(SecurePrefs.get(context).getBoolean("biometric_lock", false))
    }
    var blockShots by remember {
        mutableStateOf(SecurePrefs.get(context).getBoolean("block_screenshots", false))
    }

    var reveal by remember { mutableStateOf(false) }
    var conn by remember { mutableStateOf(ConnState.Unknown) }
    var detail by remember { mutableStateOf<String?>(null) }
    var profiles by remember { mutableStateOf(ProfileStore.all(context)) }
    val scope = AppScope

    val themeMode by UiPrefs.mode.collectAsState()
    val themeAccent by UiPrefs.accent.collectAsState()
    val scanPrompt = stringResource(R.string.settings_scan_prompt)

    var page by rememberSaveable {
        mutableStateOf(if (canCancel) SettingsPage.Home else SettingsPage.Connection)
    }
    BackHandler(enabled = canCancel && page != SettingsPage.Home) { page = SettingsPage.Home }

    fun test() {
        conn = ConnState.Testing
        detail = null
        scope.launch {
            try {
                val version = Api(server.trim(), password, fallback.trim()).ping()
                conn = ConnState.Ok
                detail = "OpenCode $version"
            } catch (e: Exception) {
                conn = ConnState.Failed
                detail = e.message
            }
        }
    }

    fun redeemPairing(target: String) {
        val value = target.trim()
        if (value.isBlank() || pairing) return
        pairing = true
        pairError = null
        // Keep the scanned/pasted value visible so a failed attempt can be retried.
        pairInput = value
        scope.launch {
            try {
                val result = Pairing.redeem(server, value)
                server = result.server
                password = result.token
                conn = ConnState.Unknown
                // Pairing is only useful once persisted: store immediately and
                // leave the connection page instead of asking for a second Save.
                onSave(server, password, fallback)
                if (canCancel) page = SettingsPage.Home
                test()
            } catch (e: Exception) {
                pairError = when {
                    e is ApiException && (e.status == 401 || e.status == 403) ->
                        context.getString(R.string.settings_pair_expired)

                    e is ApiException && e.status == 400 ->
                        context.getString(R.string.settings_pair_no_server)

                    else -> e.message ?: context.getString(R.string.settings_pair_failed)
                }
            } finally {
                pairing = false
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { redeemPairing(it) }
    }

    fun save() {
        onSave(server, password, fallback)
        profiles = ProfileStore.all(context)
        if (canCancel) page = SettingsPage.Home
    }

    val themeLabel = stringResource(
        when (themeMode) {
            "amoled" -> R.string.settings_theme_amoled
            "standard" -> R.string.settings_theme_classic
            else -> R.string.settings_theme_wallpaper
        }
    )
    val accentLabel = stringResource(
        ACCENTS.firstOrNull { it.id == themeAccent }?.label ?: R.string.settings_accent_blue
    )
    val watchdogLabel = stringResource(
        when (watchdogMinutes) {
            30 -> R.string.settings_watchdog_30
            60 -> R.string.settings_watchdog_60
            120 -> R.string.settings_watchdog_120
            else -> R.string.settings_watchdog_off
        }
    )
    val modeLabel = stringResource(
        if (persistentWatch) R.string.settings_notif_mode_always
        else R.string.settings_notif_mode_finish
    )
    val onText = stringResource(R.string.settings_summary_on)
    val offText = stringResource(R.string.settings_summary_off)
    val notConfigured = stringResource(R.string.settings_summary_not_configured)
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    val versionLabel = if (version.isBlank()) {
        "—"
    } else if (version.startsWith("v")) {
        version
    } else {
        "v$version"
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            if (forward) {
                (slideInHorizontally(tween(260)) { it / 5 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 8 } + fadeOut(tween(160)))
            } else {
                (slideInHorizontally(tween(260)) { -it / 5 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { it / 8 } + fadeOut(tween(160)))
            }
        },
        label = "settings-page"
    ) { current ->
        when (current) {
            SettingsPage.Home -> SettingsHome(
                canCancel = canCancel,
                serverSummary = if (initialServer.isBlank()) notConfigured
                else server.removePrefix("http://").removePrefix("https://").trimEnd('/'),
                notificationsSummary = if (watching) "$onText · $modeLabel" else offText,
                appearanceSummary = "$themeLabel · $accentLabel",
                automationSummary = buildString {
                    append(watchdogLabel)
                    if (budget.isNotBlank()) append(" · $").append(budget)
                },
                securitySummary = if (biometricLock || blockShots) {
                    listOfNotNull(
                        if (biometricLock) stringResource(R.string.settings_biometric_title) else null,
                        if (blockShots) stringResource(R.string.settings_screenshots_title) else null
                    ).joinToString(" · ")
                } else {
                    offText
                },
                aboutSummary = if (version.isNotBlank()) versionLabel else "",
                onBack = onCancel,
                onOpen = { page = it }
            )

            SettingsPage.Connection -> ConnectionPage(
                canCancel = canCancel,
                server = server,
                onServer = { server = it; conn = ConnState.Unknown },
                password = password,
                onPassword = { password = it; conn = ConnState.Unknown },
                reveal = reveal,
                onReveal = { reveal = !reveal },
                fallback = fallback,
                onFallback = { fallback = it; conn = ConnState.Unknown },
                advanced = showAdvanced,
                onAdvanced = { showAdvanced = !showAdvanced },
                conn = conn,
                detail = detail,
                onTest = { test() },
                onSave = { save() },
                onBack = { page = SettingsPage.Home },
                startInPairing = initialServer.isBlank(),
                pairInput = pairInput,
                onPairInput = { pairInput = it; pairError = null },
                pairError = pairError,
                pairing = pairing,
                onPair = { redeemPairing(pairInput) },
                onScan = {
                    scanLauncher.launch(
                        ScanOptions()
                            .setPrompt(scanPrompt)
                            .setBeepEnabled(false)
                            .setOrientationLocked(false)
                    )
                },
                profileName = profiles.firstOrNull { it.id == ProfileStore.activeID(context) }?.name
                    ?: stringResource(R.string.settings_profile_default),
                onProfiles = { page = SettingsPage.Profiles }
            )

            SettingsPage.Profiles -> ProfilesPage(
                context = context,
                onBack = { page = SettingsPage.Connection },
                onSwitched = { profile ->
                    server = profile.server
                    password = profile.password
                    fallback = profile.fallback
                    budget = profile.budget
                    monthBudget = profile.monthBudget
                    conn = ConnState.Unknown
                    profiles = ProfileStore.all(context)
                }
            )

            SettingsPage.Notifications -> NotificationsPage(
                context = context,
                watching = watching,
                onWatchingChange = onWatchingChange,
                notificationsEnabled = notificationsEnabled,
                persistentWatch = persistentWatch,
                onPersistentWatchChange = onPersistentWatchChange,
                onTestNotification = onTestNotification,
                onOpenSystemSettings = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                },
                onBack = { page = SettingsPage.Home }
            )

            SettingsPage.Appearance -> AppearancePage(
                themeMode = themeMode,
                onThemeMode = { UiPrefs.setMode(context, it) },
                themeAccent = themeAccent,
                onAccent = { UiPrefs.setAccent(context, it) },
                onBack = { page = SettingsPage.Home }
            )

            SettingsPage.Automation -> AutomationPage(
                watchdogMinutes = watchdogMinutes,
                onWatchdogChange = onWatchdogChange,
                budget = budget,
                onBudget = { value ->
                    budget = value.filter { it.isDigit() || it == '.' }.take(10)
                    SecurePrefs.get(context).edit().putString("budget_usd", budget).apply()
                },
                spentToday = UsageStore.today(context),
                monthBudget = monthBudget,
                onMonthBudget = { value ->
                    monthBudget = value.filter { it.isDigit() || it == '.' }.take(10)
                    SecurePrefs.get(context).edit()
                        .putString("budget_month_usd", monthBudget)
                        .apply()
                },
                pendingCount = PendingStore.all(context).size,
                onPending = { page = SettingsPage.Pending },
                onBack = { page = SettingsPage.Home }
            )

            SettingsPage.Pending -> PendingPage(
                context = context,
                onBack = { page = SettingsPage.Automation }
            )

            SettingsPage.Connections -> ConnectionsPage(
                context = context,
                onBack = { page = SettingsPage.Home }
            )

            SettingsPage.Voice -> VoicePage(
                context = context,
                onBack = { page = SettingsPage.Home }
            )

            SettingsPage.Widget -> WidgetSettingsPage(
                context = context,
                onBack = { page = SettingsPage.Home }
            )

            SettingsPage.Security -> SecurityPage(
                biometricLock = biometricLock,
                onBiometricLock = { enabled ->
                    biometricLock = enabled
                    SecurePrefs.get(context).edit()
                        .putBoolean("biometric_lock", enabled).apply()
                },
                blockShots = blockShots,
                onBlockShots = { enabled ->
                    blockShots = enabled
                    SecurePrefs.get(context).edit()
                        .putBoolean("block_screenshots", enabled).apply()
                    val window = (context as? android.app.Activity)?.window
                    if (enabled) {
                        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                },
                onBack = { page = SettingsPage.Home }
            )

            SettingsPage.About -> AboutPage(
                version = version,
                server = server,
                onBack = { page = SettingsPage.Home }
            )
        }
    }
}

/* ------------------------------------------------------------------ home */

@Composable
private fun SettingsHome(
    canCancel: Boolean,
    serverSummary: String,
    notificationsSummary: String,
    appearanceSummary: String,
    automationSummary: String,
    securitySummary: String,
    aboutSummary: String,
    onBack: () -> Unit,
    onOpen: (SettingsPage) -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 6.dp, end = 16.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canCancel) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            } else {
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_home_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    stringResource(R.string.settings_home_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var query by rememberSaveable { mutableStateOf("") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.settings_search)) },
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (query.isNotBlank()) {
                data class Match(
                    val page: SettingsPage,
                    val icon: ImageVector,
                    val title: String,
                    val supporting: String
                )

                val entries = listOf(
                    Match(
                        SettingsPage.Connection,
                        Icons.Rounded.Dns,
                        stringResource(R.string.settings_section_connection),
                        serverSummary
                    ),
                    Match(
                        SettingsPage.Profiles,
                        Icons.Rounded.Dns,
                        stringResource(R.string.settings_profiles),
                        ""
                    ),
                    Match(
                        SettingsPage.Notifications,
                        Icons.Rounded.Notifications,
                        stringResource(R.string.settings_background_title),
                        notificationsSummary
                    ),
                    Match(
                        SettingsPage.Appearance,
                        Icons.Rounded.Palette,
                        stringResource(R.string.settings_appearance_title),
                        appearanceSummary
                    ),
                    Match(
                        SettingsPage.Connections,
                        Icons.Rounded.Link,
                        stringResource(R.string.settings_connections),
                        stringResource(R.string.settings_connections_summary)
                    ),
                    Match(
                        SettingsPage.Voice,
                        Icons.Rounded.Notifications,
                        stringResource(R.string.settings_voice),
                        stringResource(R.string.settings_voice_summary)
                    ),
                    Match(
                        SettingsPage.Automation,
                        Icons.Rounded.Timer,
                        stringResource(R.string.settings_group_automation),
                        automationSummary
                    ),
                    Match(
                        SettingsPage.Security,
                        Icons.Rounded.Shield,
                        stringResource(R.string.settings_security_title),
                        securitySummary
                    ),
                    Match(
                        SettingsPage.About,
                        Icons.Rounded.Info,
                        stringResource(R.string.settings_about_title),
                        aboutSummary
                    )
                ).filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.supporting.contains(query, ignoreCase = true)
                }

                SettingsGroup {
                    if (entries.isEmpty()) {
                        Text(
                            stringResource(R.string.settings_search_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        entries.forEachIndexed { index, entry ->
                            SettingsRow(
                                icon = entry.icon,
                                title = entry.title,
                                supporting = entry.supporting,
                                onClick = { onOpen(entry.page) },
                                showDivider = index < entries.lastIndex
                            )
                        }
                    }
                }
                return@Column
            }

            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.Dns,
                    title = stringResource(R.string.settings_section_connection),
                    supporting = serverSummary,
                    onClick = { onOpen(SettingsPage.Connection) },
                    showDivider = false
                )
            }

            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_background_title),
                    supporting = notificationsSummary,
                    onClick = { onOpen(SettingsPage.Notifications) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_appearance_title),
                    supporting = appearanceSummary,
                    onClick = { onOpen(SettingsPage.Appearance) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Link,
                    title = stringResource(R.string.settings_connections),
                    supporting = stringResource(R.string.settings_connections_summary),
                    onClick = { onOpen(SettingsPage.Connections) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_voice),
                    supporting = stringResource(R.string.settings_voice_summary),
                    onClick = { onOpen(SettingsPage.Voice) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_widget),
                    supporting = stringResource(R.string.settings_widget_summary),
                    onClick = { onOpen(SettingsPage.Widget) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Timer,
                    title = stringResource(R.string.settings_group_automation),
                    supporting = automationSummary,
                    onClick = { onOpen(SettingsPage.Automation) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.settings_security_title),
                    supporting = securitySummary,
                    onClick = { onOpen(SettingsPage.Security) }
                )
            }

            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_about_title),
                    supporting = aboutSummary,
                    onClick = { onOpen(SettingsPage.About) },
                    showDivider = false
                )
            }
        }
    }
}

/* ------------------------------------------------------------ connection */

@Composable
private fun ConnectionPage(
    canCancel: Boolean,
    server: String,
    onServer: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    reveal: Boolean,
    onReveal: () -> Unit,
    fallback: String,
    onFallback: (String) -> Unit,
    advanced: Boolean,
    onAdvanced: () -> Unit,
    conn: ConnState,
    detail: String?,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    startInPairing: Boolean,
    pairInput: String,
    onPairInput: (String) -> Unit,
    pairError: String?,
    pairing: Boolean,
    onPair: () -> Unit,
    onScan: () -> Unit,
    profileName: String,
    onProfiles: () -> Unit
) {
    var pairMode by rememberSaveable { mutableStateOf(startInPairing) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PageHeader(
            title = stringResource(if (canCancel) R.string.settings_section_connection else R.string.settings_title),
            onBack = if (canCancel) onBack else null
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
        ) {
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.Dns,
                    title = stringResource(R.string.settings_profiles),
                    supporting = profileName,
                    onClick = onProfiles,
                    showDivider = false
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.settings_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            SegmentedChoice(
                options = listOf(
                    true to stringResource(R.string.settings_conn_pair),
                    false to stringResource(R.string.settings_conn_manual)
                ),
                selected = pairMode,
                onSelect = { pairMode = it }
            )
            Spacer(Modifier.height(16.dp))

            if (pairMode) {
                PairingCard(
                    pairInput = pairInput,
                    onPairInput = onPairInput,
                    pairError = pairError,
                    pairing = pairing,
                    onPair = onPair,
                    onScan = onScan
                )
                Spacer(Modifier.height(14.dp))
                ConnectionBanner(state = conn, detail = detail, onTest = onTest)
                Spacer(Modifier.height(32.dp))
            } else {
            SettingsGroup {
                Column(Modifier.padding(16.dp)) {
                    SettingsField(
                        value = server,
                        onValueChange = onServer,
                        label = stringResource(R.string.settings_server_url),
                        placeholder = "http://192.168.1.10:28894",
                        leading = Icons.Rounded.Dns,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsField(
                        value = password,
                        onValueChange = onPassword,
                        label = stringResource(R.string.settings_password),
                        leading = Icons.Rounded.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        visualTransformation =
                            if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = {
                            IconButton(onClick = onReveal) {
                                Icon(
                                    if (reveal) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                    contentDescription = stringResource(
                                        if (reveal) R.string.settings_hide_password
                                        else R.string.settings_show_password
                                    ),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Dns,
                    title = stringResource(R.string.settings_advanced),
                    supporting = stringResource(R.string.settings_fallback_url),
                    onClick = onAdvanced,
                    trailing = {
                        Icon(
                            if (advanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    showDivider = false
                )
                AnimatedVisibility(visible = advanced) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        SettingsField(
                            value = fallback,
                            onValueChange = onFallback,
                            label = stringResource(R.string.settings_fallback_url),
                            placeholder = "https://…opentunnel.xyz",
                            leading = Icons.Rounded.Dns,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.settings_fallback_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            ConnectionBanner(state = conn, detail = detail, onTest = onTest)

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onSave,
                enabled = server.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    stringResource(
                        if (canCancel) R.string.action_save else R.string.action_connect
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PairingCard(
    pairInput: String,
    onPairInput: (String) -> Unit,
    pairError: String?,
    pairing: Boolean,
    onPair: () -> Unit,
    onScan: () -> Unit
) {
    SettingsGroup {
        Column(Modifier.padding(16.dp)) {
            Button(
                onClick = onScan,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(
                    Icons.Rounded.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_scan_qr))
            }
            Spacer(Modifier.height(14.dp))
            SettingsField(
                value = pairInput,
                onValueChange = onPairInput,
                label = stringResource(R.string.settings_pair_field),
                leading = Icons.Rounded.QrCode,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            )
            if (pairError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    pairError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPair,
                enabled = pairInput.isNotBlank() && !pairing,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (pairing) R.string.settings_pairing else R.string.action_pair
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.settings_pair_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfilesPage(
    context: Context,
    onBack: () -> Unit,
    onSwitched: (ServerProfile) -> Unit
) {
    var profiles by remember { mutableStateOf(ProfileStore.all(context)) }
    var active by remember { mutableStateOf(ProfileStore.activeID(context)) }
    var showAdd by remember { mutableStateOf(false) }
    var addName by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<ServerProfile?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<ServerProfile?>(null) }
    var defaultsFor by remember { mutableStateOf<ServerProfile?>(null) }

    PageScaffold(
        title = stringResource(R.string.settings_profiles),
        onBack = onBack
    ) {
        SettingsGroup {
            profiles.forEachIndexed { index, profile ->
                SettingsRow(
                    icon = if (profile.id == active) Icons.Rounded.Check else Icons.Rounded.Dns,
                    title = profile.name,
                    supporting = profile.server
                        .removePrefix("http://")
                        .removePrefix("https://")
                        .trimEnd('/'),
                    onClick = {
                        ProfileStore.switchTo(context, profile.id)
                        active = profile.id
                        profiles = ProfileStore.all(context)
                        onSwitched(profile)
                    },
                    trailing = {
                        Row {
                            IconButton(
                                onClick = {
                                    renaming = profile
                                    renameValue = profile.name
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.settings_profile_rename),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (profiles.size > 1) {
                                IconButton(
                                    onClick = { deleting = profile },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    },
                    showDivider = index < profiles.lastIndex
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                addName = ""
                showAdd = true
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(stringResource(R.string.settings_profile_add))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.settings_profile_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(12.dp))
        val activeProfile = profiles.firstOrNull { it.id == active }
        OutlinedButton(
            onClick = { activeProfile?.let { defaultsFor = it } },
            enabled = activeProfile != null,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_profile_defaults))
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.settings_profile_add)) },
            text = {
                OutlinedTextField(
                    value = addName,
                    onValueChange = { addName = it },
                    label = { Text(stringResource(R.string.settings_profile_name)) },
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (addName.isNotBlank()) {
                        val created = ProfileStore.addFromCurrent(context, addName.trim())
                        profiles = ProfileStore.all(context)
                        active = created.id
                        onSwitched(created)
                    }
                    showAdd = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    renaming?.let { profile ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.settings_profile_rename)) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text(stringResource(R.string.settings_profile_name)) },
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameValue.isNotBlank()) {
                        ProfileStore.save(context, profile.copy(name = renameValue.trim()))
                        profiles = ProfileStore.all(context)
                        profiles.firstOrNull { it.id == active }?.let(onSwitched)
                    }
                    renaming = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.settings_profile_delete_title)) },
            text = {
                Text(stringResource(R.string.settings_profile_delete_body, profile.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    ProfileStore.delete(context, profile.id)
                    profiles = ProfileStore.all(context)
                    active = ProfileStore.activeID(context)
                    profiles.firstOrNull { it.id == active }?.let(onSwitched)
                    deleting = null
                }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    defaultsFor?.let { profile ->
        ProfileDefaultsDialog(
            context = context,
            profile = profile,
            onDismiss = { defaultsFor = null },
            onSaved = { updated ->
                ProfileStore.save(context, updated)
                profiles = ProfileStore.all(context)
                onSwitched(updated)
                defaultsFor = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileDefaultsDialog(
    context: Context,
    profile: ServerProfile,
    onDismiss: () -> Unit,
    onSaved: (ServerProfile) -> Unit
) {
    val prefs = SecurePrefs.get(context)
    val api = remember {
        Api(
            prefs.getString("server", "").orEmpty(),
            prefs.getString("password", "").orEmpty(),
            prefs.getString("fallback_server", "").orEmpty()
        )
    }
    var agents by remember { mutableStateOf<List<AgentOption>?>(null) }
    var models by remember { mutableStateOf<List<ModelOption>?>(null) }
    var agent by remember { mutableStateOf(profile.agentID) }
    var modelID by remember { mutableStateOf(profile.modelID) }
    var provider by remember { mutableStateOf(profile.providerID) }

    LaunchedEffect(Unit) {
        agents = runCatching { api.agents() }.getOrDefault(emptyList())
        models = runCatching { api.models() }.getOrDefault(emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_profile_defaults)) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.settings_profile_agent),
                    style = MaterialTheme.typography.labelLarge
                )
                val agentList = agents
                if (agentList == null) {
                    Text(
                        stringResource(R.string.settings_connections_loading),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        agentList.forEach { option ->
                            FilterChip(
                                selected = agent == option.id,
                                onClick = { agent = option.id },
                                label = { Text(option.name) }
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.settings_profile_model),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 6.dp)
                )
                val modelList = models
                if (modelList == null) {
                    Text(
                        stringResource(R.string.settings_connections_loading),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        modelList.forEach { option ->
                            FilterChip(
                                selected = modelID == option.id && provider == option.providerID,
                                onClick = {
                                    modelID = option.id
                                    provider = option.providerID
                                },
                                label = { Text(option.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSaved(
                    profile.copy(
                        agentID = agent,
                        modelID = modelID,
                        providerID = provider
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/* --------------------------------------------------------- notifications */

@Composable
private fun NotificationsPage(
    context: Context,
    watching: Boolean,
    onWatchingChange: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    persistentWatch: Boolean,
    onPersistentWatchChange: (Boolean) -> Unit,
    onTestNotification: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onBack: () -> Unit
) {
    var completionSound by remember {
        mutableStateOf(SecurePrefs.get(context).getBoolean("completion_sound", false))
    }
    PageScaffold(
        title = stringResource(R.string.settings_background_title),
        onBack = onBack
    ) {
        if (!notificationsEnabled) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_notifications_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(
                        onClick = onOpenSystemSettings,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.settings_open_notification_settings))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        SettingsGroup {
            SwitchRow(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.settings_background_title),
                supporting = stringResource(R.string.settings_background_body),
                checked = watching,
                onChange = onWatchingChange
            )
            SettingsDivider()
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_notif_mode_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_notif_mode_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                SegmentedChoice(
                    options = listOf(
                        false to stringResource(R.string.settings_notif_mode_finish),
                        true to stringResource(R.string.settings_notif_mode_always)
                    ),
                    selected = persistentWatch,
                    onSelect = onPersistentWatchChange
                )
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Send,
                title = stringResource(R.string.settings_test_notification),
                onClick = onTestNotification
            )
            SettingsDivider()
            SwitchRow(
                icon = Icons.Rounded.VolumeUp,
                title = stringResource(R.string.settings_completion_sound),
                supporting = stringResource(R.string.settings_completion_sound_body),
                checked = completionSound,
                onChange = { enabled ->
                    completionSound = enabled
                    SecurePrefs.get(context).edit()
                        .putBoolean("completion_sound", enabled)
                        .apply()
                }
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.settings_quick_prompts_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        val prefs = SecurePrefs.get(context)
        var continueText by remember {
            mutableStateOf(prefs.getString("quick_prompt_continue", "").orEmpty())
        }
        var explainText by remember {
            mutableStateOf(prefs.getString("quick_prompt_explain", "").orEmpty())
        }
        var retryText by remember {
            mutableStateOf(prefs.getString("quick_prompt_retry", "").orEmpty())
        }
        SettingsGroup {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = continueText,
                    onValueChange = { value ->
                        continueText = value
                        prefs.edit().putString("quick_prompt_continue", value).apply()
                    },
                    label = { Text(stringResource(R.string.notif_quick_continue)) },
                    placeholder = { Text("Continue.") },
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = explainText,
                    onValueChange = { value ->
                        explainText = value
                        prefs.edit().putString("quick_prompt_explain", value).apply()
                    },
                    label = { Text(stringResource(R.string.notif_quick_explain)) },
                    placeholder = {
                        Text("Explain what you changed and why, in a short summary.")
                    },
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = retryText,
                    onValueChange = { value ->
                        retryText = value
                        prefs.edit().putString("quick_prompt_retry", value).apply()
                    },
                    label = { Text(stringResource(R.string.notif_quick_retry)) },
                    placeholder = { Text("That did not work. Try again differently.") },
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/* ------------------------------------------------------------ appearance */

@Composable
private fun AppearancePage(
    themeMode: String,
    onThemeMode: (String) -> Unit,
    themeAccent: String,
    onAccent: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val showSubagents by UiPrefs.showSubagents.collectAsState()
    PageScaffold(
        title = stringResource(R.string.settings_appearance_title),
        onBack = onBack
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomEnd = 6.dp,
                        bottomStart = 18.dp
                    ),
                    modifier = Modifier.fillMaxWidth(0.82f)
                ) {
                    Text(
                        stringResource(R.string.settings_preview_user),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Text(
                        stringResource(R.string.settings_preview_assistant),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        SegmentedChoice(
            options = listOf(
                "dynamic" to stringResource(R.string.settings_theme_wallpaper),
                "standard" to stringResource(R.string.settings_theme_classic),
                "amoled" to stringResource(R.string.settings_theme_amoled)
            ),
            selected = themeMode,
            onSelect = onThemeMode
        )

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.settings_accent),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ACCENTS.forEach { accent ->
                val selected = accent.id == themeAccent
                val accentLabelText = stringResource(accent.label)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { onAccent(accent.id) },
                        shape = CircleShape,
                        color = accent.color,
                        border = if (selected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                        } else {
                            null
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .semantics { contentDescription = accentLabelText }
                    ) {
                        if (selected) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF101319),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(accent.label),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SettingsGroup {
            SwitchRow(
                icon = Icons.Rounded.Group,
                title = stringResource(R.string.settings_subagents_title),
                supporting = stringResource(R.string.settings_subagents_body),
                checked = showSubagents,
                onChange = { enabled -> UiPrefs.setShowSubagents(context, enabled) },
                showDivider = false
            )
        }
    }
}

/* ------------------------------------------------------------ automation */

@Composable
private fun AutomationPage(
    watchdogMinutes: Int,
    onWatchdogChange: (Int) -> Unit,
    budget: String,
    onBudget: (String) -> Unit,
    spentToday: Double,
    monthBudget: String,
    onMonthBudget: (String) -> Unit,
    pendingCount: Int,
    onPending: () -> Unit,
    onBack: () -> Unit
) {
    PageScaffold(
        title = stringResource(R.string.settings_group_automation),
        onBack = onBack
    ) {
        SettingsGroup {
            SettingsRow(
                icon = Icons.Rounded.Send,
                title = stringResource(R.string.settings_pending),
                supporting = if (pendingCount > 0) {
                    stringResource(R.string.settings_pending_count, pendingCount)
                } else {
                    stringResource(R.string.settings_pending_empty)
                },
                onClick = onPending
            )
            SettingsDivider()
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_watchdog_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_watchdog_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                SegmentedChoice(
                    options = listOf(
                        0 to stringResource(R.string.settings_watchdog_off),
                        30 to stringResource(R.string.settings_watchdog_30),
                        60 to stringResource(R.string.settings_watchdog_60),
                        120 to stringResource(R.string.settings_watchdog_120)
                    ),
                    selected = watchdogMinutes,
                    onSelect = onWatchdogChange
                )
            }
            SettingsDivider()
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_budget_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_budget_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = budget,
                    onValueChange = onBudget,
                    label = { Text(stringResource(R.string.settings_budget_field)) },
                    placeholder = { Text("5.00") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_spend_today, formatCost(spentToday)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.settings_budget_month_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = monthBudget,
                    onValueChange = onMonthBudget,
                    label = { Text(stringResource(R.string.settings_budget_month_field)) },
                    placeholder = { Text("50.00") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/* --------------------------------------------------------------- pending */

@Composable
private fun PendingPage(context: Context, onBack: () -> Unit) {
    var items by remember { mutableStateOf(PendingStore.all(context)) }
    val prefs = SecurePrefs.get(context)
    val api = remember {
        Api(
            prefs.getString("server", "").orEmpty(),
            prefs.getString("password", "").orEmpty(),
            prefs.getString("fallback_server", "").orEmpty()
        )
    }
    val scope = rememberCoroutineScope()

    PageScaffold(
        title = stringResource(R.string.settings_pending),
        onBack = onBack
    ) {
        if (items.isEmpty()) {
            Text(
                stringResource(R.string.settings_pending_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            SettingsGroup {
                items.forEachIndexed { index, prompt ->
                    SettingsRow(
                        icon = Icons.Rounded.Send,
                        title = prompt.text.replace('\n', ' '),
                        supporting = listOf(
                            prompt.title,
                            prompt.sessionID,
                            if (prompt.attachments.isNotEmpty()) {
                                "${prompt.attachments.size} files"
                            } else {
                                ""
                            }
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        trailing = {
                            Row {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val files = prompt.attachments.mapNotNull { att ->
                                                runCatching {
                                                    Attachment(
                                                        att.name,
                                                        att.mime,
                                                        android.util.Base64.encodeToString(
                                                            java.io.File(att.path).readBytes(),
                                                            android.util.Base64.NO_WRAP
                                                        )
                                                    )
                                                }.getOrNull()
                                            }
                                            val ok = runCatching {
                                                api.send(
                                                    sessionID = prompt.sessionID,
                                                    text = prompt.text,
                                                    files = files,
                                                    skillIDs = prompt.skillIDs
                                                )
                                            }.isSuccess
                                            if (ok) {
                                                PendingStore.remove(context, prompt.id)
                                                prompt.attachments.forEach {
                                                    runCatching { java.io.File(it.path).delete() }
                                                }
                                                items = PendingStore.all(context)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Send,
                                        contentDescription = stringResource(R.string.action_send),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        PendingStore.remove(context, prompt.id)
                                        prompt.attachments.forEach {
                                            runCatching { java.io.File(it.path).delete() }
                                        }
                                        items = PendingStore.all(context)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        showDivider = index < items.lastIndex
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.settings_pending_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/* ------------------------------------------------------------ connections */

@Composable
private fun ConnectionsPage(context: Context, onBack: () -> Unit) {
    val prefs = SecurePrefs.get(context)
    val api = remember {
        Api(
            prefs.getString("server", "").orEmpty(),
            prefs.getString("password", "").orEmpty(),
            prefs.getString("fallback_server", "").orEmpty()
        )
    }
    var mcp by remember { mutableStateOf<List<McpServer>?>(null) }
    var resources by remember { mutableStateOf<List<McpResource>>(emptyList()) }
    var integrations by remember { mutableStateOf<List<IntegrationItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var keyFor by remember { mutableStateOf<IntegrationItem?>(null) }
    var keyValue by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            try {
                mcp = api.mcpServers()
                integrations = api.integrations()
                resources = runCatching { api.mcpResources() }.getOrDefault(emptyList())
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    PageScaffold(
        title = stringResource(R.string.settings_connections),
        onBack = onBack
    ) {
        Text(
            stringResource(R.string.settings_connections_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.settings_mcp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
            val servers = mcp
            if (servers == null) {
                Text(
                    stringResource(R.string.settings_connections_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (servers.isEmpty()) {
                Text(
                    stringResource(R.string.settings_connections_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                servers.forEachIndexed { index, server ->
                    val connected = server.status.contains("connect", ignoreCase = true)
                    SettingsRow(
                        icon = Icons.Rounded.Link,
                        title = server.name,
                        supporting = server.status,
                        trailing = {
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching {
                                        if (connected) api.mcpDisconnect(server.name)
                                        else api.mcpConnect(server.name)
                                    }
                                    reload()
                                }
                            }) {
                                Text(
                                    stringResource(
                                        if (connected) R.string.settings_disconnect
                                        else R.string.settings_connect
                                    )
                                )
                            }
                        },
                        showDivider = index < servers.lastIndex
                    )
                }
            }
        }

        if (resources.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.settings_mcp_resources),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            SettingsGroup {
                val visible = resources.take(20)
                visible.forEachIndexed { index, resource ->
                    SettingsRow(
                        icon = Icons.Rounded.Link,
                        title = resource.name,
                        supporting = listOf(resource.server, resource.uri)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        showDivider = index < visible.lastIndex
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.settings_integrations),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.settings_search_integrations)) },
            singleLine = true,
            colors = fieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        val filtered = integrations
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedByDescending { it.connected }
            .take(if (query.isBlank()) 12 else 30)
        Spacer(Modifier.height(10.dp))
        SettingsGroup {
            if (filtered.isEmpty()) {
                Text(
                    stringResource(R.string.settings_connections_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                filtered.forEachIndexed { index, item ->
                    SettingsRow(
                        icon = Icons.Rounded.Link,
                        title = item.name,
                        supporting = if (item.connected) {
                            stringResource(R.string.settings_connected_label)
                        } else {
                            val base = item.methods.joinToString(" / ")
                            if (item.envNames.isNotEmpty()) {
                                "$base · ${item.envNames.joinToString(", ")}"
                            } else {
                                base
                            }
                        },
                        onClick = {
                            if (!item.connected && item.methods.contains("key")) {
                                keyFor = item
                                keyValue = ""
                            }
                        },
                        showDivider = index < filtered.lastIndex
                    )
                }
            }
        }
    }

    keyFor?.let { item ->
        AlertDialog(
            onDismissRequest = { keyFor = null },
            title = { Text(item.name) },
            text = {
                OutlinedTextField(
                    value = keyValue,
                    onValueChange = { keyValue = it },
                    label = { Text(stringResource(R.string.settings_integration_key)) },
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (keyValue.isNotBlank()) {
                        scope.launch {
                            runCatching { api.connectIntegrationKey(item.id, keyValue.trim()) }
                            keyFor = null
                            reload()
                        }
                    }
                }) { Text(stringResource(R.string.settings_connect)) }
            },
            dismissButton = {
                TextButton(onClick = { keyFor = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/* ------------------------------------------------------------------ voice */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoicePage(context: Context, onBack: () -> Unit) {
    var language by remember {
        mutableStateOf(SecurePrefs.get(context).getString("voice_language", "").orEmpty())
    }
    var voiceName by remember {
        mutableStateOf(SecurePrefs.get(context).getString("voice_name", "").orEmpty())
    }
    val tts = remember { android.speech.tts.TextToSpeech(context) { } }
    var voices by remember { mutableStateOf<List<android.speech.tts.Voice>>(emptyList()) }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    LaunchedEffect(language) {
        voices = runCatching {
            val tag = language.ifBlank { java.util.Locale.getDefault().toLanguageTag() }
            tts.voices
                ?.filter { it.locale.toLanguageTag().startsWith(tag.take(2)) }
                ?.sortedBy { it.name }
                ?.take(16)
                ?: emptyList()
        }.getOrDefault(emptyList())
    }
    PageScaffold(
        title = stringResource(R.string.settings_voice),
        onBack = onBack
    ) {
        Text(
            stringResource(R.string.settings_voice_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        SettingsGroup {
            Column(Modifier.padding(16.dp)) {
                SettingsField(
                    value = language,
                    onValueChange = { value ->
                        language = value
                        SecurePrefs.get(context).edit()
                            .putString("voice_language", value.trim())
                            .apply()
                    },
                    label = stringResource(R.string.settings_voice_language),
                    placeholder = "en-US"
                )
                if (voices.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.settings_voice_engine),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        voices.forEach { voice ->
                            FilterChip(
                                selected = voiceName == voice.name,
                                onClick = {
                                    voiceName = if (voiceName == voice.name) "" else voice.name
                                    SecurePrefs.get(context).edit()
                                        .putString("voice_name", voiceName)
                                        .apply()
                                },
                                label = {
                                    Text(
                                        voice.name.substringAfterLast('-').take(24),
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ----------------------------------------------------------------- widget */

@Composable
private fun WidgetSettingsPage(context: Context, onBack: () -> Unit) {
    val prefs = SecurePrefs.get(context)
    var showTitles by remember {
        mutableStateOf(prefs.getBoolean("widget_show_titles", true))
    }
    var showStop by remember {
        mutableStateOf(prefs.getBoolean("widget_show_stop", true))
    }
    PageScaffold(
        title = stringResource(R.string.settings_widget),
        onBack = onBack
    ) {
        SettingsGroup {
            SwitchRow(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.widget_show_titles),
                supporting = "",
                checked = showTitles,
                onChange = { enabled ->
                    showTitles = enabled
                    prefs.edit().putBoolean("widget_show_titles", enabled).apply()
                    OpenCodeWidgetProvider.refresh(context)
                }
            )
            SettingsDivider()
            SwitchRow(
                icon = Icons.Rounded.Send,
                title = stringResource(R.string.widget_show_stop),
                supporting = "",
                checked = showStop,
                onChange = { enabled ->
                    showStop = enabled
                    prefs.edit().putBoolean("widget_show_stop", enabled).apply()
                    OpenCodeWidgetProvider.refresh(context)
                },
                showDivider = false
            )
        }
    }
}

/* -------------------------------------------------------------- security */

@Composable
private fun SecurityPage(
    biometricLock: Boolean,
    onBiometricLock: (Boolean) -> Unit,
    blockShots: Boolean,
    onBlockShots: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    PageScaffold(
        title = stringResource(R.string.settings_security_title),
        onBack = onBack
    ) {
        SettingsGroup {
            SwitchRow(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.settings_biometric_title),
                supporting = stringResource(R.string.settings_biometric_body),
                checked = biometricLock,
                onChange = onBiometricLock
            )
            SettingsDivider()
            SwitchRow(
                icon = Icons.Rounded.Lock,
                title = stringResource(R.string.settings_screenshots_title),
                supporting = stringResource(R.string.settings_screenshots_body),
                checked = blockShots,
                onChange = onBlockShots,
                showDivider = false
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.settings_security_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/* ------------------------------------------------------------------ about */

@Composable
private fun AboutPage(version: String, server: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val versionLabel = if (version.isBlank()) {
        "—"
    } else if (version.startsWith("v")) {
        version
    } else {
        "v$version"
    }
    val diagnosticsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val prefs = SecurePrefs.get(context)
            val text = buildString {
                appendLine("OpenCode Mobile ${version.ifBlank { "?" }}")
                appendLine(
                    "Android ${android.os.Build.VERSION.RELEASE} " +
                        "(SDK ${android.os.Build.VERSION.SDK_INT})"
                )
                appendLine("Device ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("Server ${prefs.getString("server", "").orEmpty()}")
                appendLine("Fallback ${prefs.getString("fallback_server", "").orEmpty()}")
                appendLine("Watching ${prefs.getBoolean("watch", false)}")
                appendLine(
                    "Persistent notification ${prefs.getBoolean("watch_persistent", false)}"
                )
                appendLine("Watchdog ${prefs.getInt("watchdog_minutes", 0)} min")
                appendLine("Pending prompts ${PendingStore.all(context).size}")
                appendLine("Profiles ${ProfileStore.all(context).size}")
                appendLine("Theme ${UiPrefs.mode.value} / ${UiPrefs.accent.value}")
            }
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(text.toByteArray())
                }
            }
        }
    }
    PageScaffold(
        title = stringResource(R.string.settings_about_title),
        onBack = onBack
    ) {
        SettingsGroup {
            SettingsRow(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.settings_version),
                supporting = versionLabel,
                onClick = {
                    if (version.isNotBlank()) {
                        clipboard.setText(AnnotatedString(version))
                    }
                }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Dns,
                title = stringResource(R.string.settings_server_row),
                supporting = server.removePrefix("http://").removePrefix("https://").trimEnd('/'),
                showDivider = false
            )
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            SettingsRow(
                icon = Icons.Rounded.OpenInNew,
                title = stringResource(R.string.settings_github),
                supporting = "github.com/michaelatamuk/opencode-mobile",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse(
                                    "https://github.com/michaelatamuk/opencode-mobile"
                                )
                            )
                        )
                    }
                }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.settings_export_diagnostics),
                supporting = stringResource(R.string.settings_export_diagnostics_hint),
                onClick = {
                    diagnosticsLauncher.launch("opencode-diagnostics.txt")
                },
                showDivider = false
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.settings_about_body) + " " +
                stringResource(R.string.settings_about_storage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/* ------------------------------------------------------------- components */

@Composable
private fun PageScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(Modifier.fillMaxHeight().widthIn(max = 760.dp)) {
            PageHeader(title = title, onBack = onBack)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, onBack: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        } else {
            Spacer(Modifier.width(10.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
                .padding(start = 16.dp, end = 14.dp, top = 13.dp, bottom = 13.dp)
                .heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (!supporting.isNullOrBlank()) {
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) SettingsDivider()
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    supporting: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    SettingsRow(
        icon = icon,
        title = title,
        supporting = supporting,
        onClick = { onChange(!checked) },
        trailing = { Switch(checked = checked, onCheckedChange = onChange) },
        showDivider = showDivider
    )
}

@Composable
private fun <T> SegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option.first == selected,
                onClick = { onSelect(option.first) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    option.second,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String? = null,
    leading: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) ({ Text(placeholder) }) else null,
        singleLine = true,
        leadingIcon = leading?.let {
            {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        },
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        colors = fieldColors(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ConnectionBanner(state: ConnState, detail: String?, onTest: () -> Unit) {
    val accent = when (state) {
        ConnState.Ok -> MaterialTheme.colorScheme.secondary
        ConnState.Failed -> MaterialTheme.colorScheme.error
        ConnState.Testing -> MaterialTheme.colorScheme.primary
        ConnState.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val title = when (state) {
        ConnState.Ok -> stringResource(R.string.settings_connected)
        ConnState.Failed -> stringResource(R.string.settings_not_reachable)
        ConnState.Testing -> stringResource(R.string.settings_checking)
        ConnState.Unknown -> stringResource(R.string.settings_not_tested)
    }
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(accent, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (!detail.isNullOrBlank()) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = onTest) {
                Text(stringResource(R.string.action_test))
            }
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent
)
