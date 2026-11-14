package com.opencode.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.opencode.mobile.Api
import com.opencode.mobile.Pairing
import com.opencode.mobile.R
import com.opencode.mobile.AppScope
import com.opencode.mobile.UiPrefs
import kotlinx.coroutines.launch

private enum class ConnState { Unknown, Testing, Ok, Failed }

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
    var server by remember { mutableStateOf(initialServer.ifBlank { "http://192.168.1.10:28894" }) }
    var password by remember { mutableStateOf(initialPassword) }
    var fallback by remember { mutableStateOf(initialFallback) }
    var pairInput by remember { mutableStateOf("") }
    var pairError by remember { mutableStateOf<String?>(null) }
    var pairing by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(ConnState.Unknown) }
    var detail by remember { mutableStateOf<String?>(null) }
    val scope = AppScope
    val context = LocalContext.current
    val themeMode by UiPrefs.mode.collectAsState()
    val themeAccent by UiPrefs.accent.collectAsState()
    val scanPrompt = stringResource(R.string.settings_scan_prompt)

    fun test() {
        state = ConnState.Testing
        detail = null
        scope.launch {
            try {
                val version = Api(server.trim(), password, fallback.trim()).ping()
                state = ConnState.Ok
                detail = "OpenCode $version"
            } catch (e: Exception) {
                state = ConnState.Failed
                detail = e.message
            }
        }
    }

    fun redeemPairing(target: String) {
        val value = target.trim()
        if (value.isBlank() || pairing) return
        pairing = true
        pairError = null
        scope.launch {
            try {
                val token = Pairing.redeem(server, value)
                if (value.contains("/auth/connect/")) {
                    server = value.substringBefore("/auth/connect/")
                }
                password = token
                pairInput = value
                state = ConnState.Unknown
                test()
            } catch (e: Exception) {
                pairError = e.message
            } finally {
                pairing = false
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { redeemPairing(it) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canCancel) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Spacer(Modifier.weight(1f))
            if (canCancel) {
                TextButton(onClick = { onSave(server, password, fallback) }) { Text(stringResource(R.string.action_save)) }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            SectionLabel(stringResource(R.string.settings_section_connection))
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.settings_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))

            FieldCard {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it; state = ConnState.Unknown },
                    label = { Text(stringResource(R.string.settings_server_url)) },
                    placeholder = { Text("http://192.168.1.10:28894") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; state = ConnState.Unknown },
                    label = { Text(stringResource(R.string.settings_password)) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { reveal = !reveal }) {
                            Icon(
                                if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = stringResource(if (reveal) R.string.settings_hide_password else R.string.settings_show_password),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    visualTransformation =
                        if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = fallback,
                    onValueChange = { fallback = it; state = ConnState.Unknown },
                    label = { Text(stringResource(R.string.settings_fallback_url)) },
                    placeholder = { Text("https://…opentunnel.xyz") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.settings_fallback_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))
            StatusPill(state, detail)

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { test() },
                    enabled = server.isNotBlank() && state != ConnState.Testing,
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(if (state == ConnState.Testing) R.string.action_testing else R.string.action_test)) }
                Button(
                    onClick = { onSave(server, password, fallback) },
                    enabled = server.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(if (canCancel) R.string.action_save else R.string.action_connect)) }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_pair_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_pair_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pairInput,
                        onValueChange = { pairInput = it; pairError = null },
                        label = { Text(stringResource(R.string.settings_pair_field)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pairError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            pairError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { redeemPairing(pairInput) },
                            enabled = pairInput.isNotBlank() && !pairing,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                stringResource(
                                    if (pairing) R.string.settings_pairing else R.string.action_pair
                                )
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                scanLauncher.launch(
                                    ScanOptions()
                                        .setPrompt(scanPrompt)
                                        .setBeepEnabled(false)
                                        .setOrientationLocked(false)
                                )
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.settings_scan_qr))
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_background_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.settings_background_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = watching, onCheckedChange = onWatchingChange)
                    }

                    if (!notificationsEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.settings_notifications_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = onTestNotification,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.settings_test_notification))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !persistentWatch,
                            onClick = { onPersistentWatchChange(false) },
                            label = { Text(stringResource(R.string.settings_notif_mode_finish)) }
                        )
                        FilterChip(
                            selected = persistentWatch,
                            onClick = { onPersistentWatchChange(true) },
                            label = { Text(stringResource(R.string.settings_notif_mode_always)) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_appearance_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "dynamic" to R.string.settings_theme_wallpaper,
                            "standard" to R.string.settings_theme_classic,
                            "amoled" to R.string.settings_theme_amoled
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { UiPrefs.setMode(context, mode) },
                                label = { Text(stringResource(label)) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.settings_accent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            "blue" to Color(0xFF8FB4FF),
                            "mint" to Color(0xFF9FE7C3),
                            "violet" to Color(0xFFE7C6FF),
                            "amber" to Color(0xFFFFD59B)
                        ).forEach { (name, color) ->
                            Surface(
                                onClick = { UiPrefs.setAccent(context, name) },
                                shape = CircleShape,
                                color = color,
                                modifier = Modifier.size(30.dp)
                            ) {
                                if (themeAccent == name) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF101319),
                                        modifier = Modifier.padding(7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0 to R.string.settings_watchdog_off,
                            30 to R.string.settings_watchdog_30,
                            60 to R.string.settings_watchdog_60,
                            120 to R.string.settings_watchdog_120
                        ).forEach { (minutes, label) ->
                            FilterChip(
                                selected = watchdogMinutes == minutes,
                                onClick = { onWatchdogChange(minutes) },
                                label = { Text(stringResource(label)) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.settings_about_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_about_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.settings_about_storage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun FieldCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) { content() }
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

@Composable
private fun StatusPill(state: ConnState, detail: String?) {
    val color = when (state) {
        ConnState.Ok -> MaterialTheme.colorScheme.secondary
        ConnState.Failed -> MaterialTheme.colorScheme.error
        ConnState.Testing -> MaterialTheme.colorScheme.primary
        ConnState.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when (state) {
        ConnState.Ok -> stringResource(R.string.settings_connected)
        ConnState.Failed -> stringResource(R.string.settings_not_reachable)
        ConnState.Testing -> stringResource(R.string.settings_checking)
        ConnState.Unknown -> stringResource(R.string.settings_not_tested)
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 }
    ) {
        Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp)) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state) {
                    ConnState.Ok -> Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )

                    ConnState.Failed -> Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )

                    else -> Box(
                        Modifier.size(8.dp).background(color, CircleShape)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = color)
                    if (!detail.isNullOrBlank()) {
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
