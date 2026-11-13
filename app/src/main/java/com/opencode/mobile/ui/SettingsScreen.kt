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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencode.mobile.Api
import com.opencode.mobile.AppScope
import kotlinx.coroutines.launch

private enum class ConnState { Unknown, Testing, Ok, Failed }

@Composable
fun SettingsScreen(
    initialServer: String,
    initialPassword: String,
    canCancel: Boolean,
    watching: Boolean,
    onWatchingChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var server by remember { mutableStateOf(initialServer.ifBlank { "http://192.168.1.10:28894" }) }
    var password by remember { mutableStateOf(initialPassword) }
    var reveal by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(ConnState.Unknown) }
    var detail by remember { mutableStateOf<String?>(null) }
    val scope = AppScope

    fun test() {
        state = ConnState.Testing
        detail = null
        scope.launch {
            try {
                val version = Api(server.trim(), password).ping()
                state = ConnState.Ok
                detail = "OpenCode $version"
            } catch (e: Exception) {
                state = ConnState.Failed
                detail = e.message
            }
        }
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
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Spacer(Modifier.weight(1f))
            if (canCancel) {
                TextButton(onClick = { onSave(server, password) }) { Text("Save") }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            SectionLabel("Connection")
            Spacer(Modifier.height(6.dp))
            Text("Your server", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(10.dp))
            Text(
                "Use the LAN address on the same Wi-Fi, or the tunnel URL when you are away.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))

            FieldCard {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it; state = ConnState.Unknown },
                    label = { Text("Server URL") },
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
                    label = { Text("Password") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { reveal = !reveal }) {
                            Icon(
                                if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (reveal) "Hide password" else "Show password",
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
            }

            Spacer(Modifier.height(14.dp))
            StatusPill(state, detail)

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { test() },
                    enabled = server.isNotBlank() && state != ConnState.Testing,
                    shape = RoundedCornerShape(14.dp)
                ) { Text(if (state == ConnState.Testing) "Testing…" else "Test connection") }
                Button(
                    onClick = { onSave(server, password) },
                    enabled = server.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(if (canCancel) "Save" else "Connect") }
            }

            Spacer(Modifier.height(28.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Background notifications", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Notify me when a session finishes while I'm elsewhere. " +
                                "Keeps a low-priority notification while watching.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = watching, onCheckedChange = onWatchingChange)
                }
            }

            Spacer(Modifier.height(28.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "OpenCode Mobile talks to your OpenCode server over its HTTP API. " +
                            "The username is always \"opencode\"; only the password is needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Credentials are stored on this device only.",
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
        ConnState.Ok -> "Connected"
        ConnState.Failed -> "Not reachable"
        ConnState.Testing -> "Checking…"
        ConnState.Unknown -> "Not tested yet"
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
