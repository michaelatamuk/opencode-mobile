$ErrorActionPreference = "Stop"
Set-Location "C:\Workspace\opencode-mobile\app\src\main\java\com\opencode\mobile\ui"

$files = @{
    "SessionListScreen.kt" = @(
        @('contentDescription = "Refresh"', 'contentDescription = stringResource(R.string.action_refresh)'),
        @('contentDescription = "Settings"', 'contentDescription = stringResource(R.string.action_settings)'),
        @('SectionLabel("OpenCode")', 'SectionLabel(stringResource(R.string.app_name))'),
        @('Text("Sessions", style', 'Text(stringResource(R.string.sessions_title), style'),
        @('"Reconnecting. ${state.error}"', 'stringResource(R.string.sessions_reconnecting, state.error ?: "")'),
        @('"running"', 'stringResource(R.string.session_running)'),
        @('Text("New session")', 'Text(stringResource(R.string.action_new_session))'),
        @('"Can''t reach your server"', 'stringResource(R.string.sessions_unreachable_title)'),
        @('"No sessions yet"', 'stringResource(R.string.sessions_empty_title)'),
        @('error ?: "Start a session from the TUI, or create one here."', 'error ?: stringResource(R.string.sessions_empty_body)'),
        @('Text("Retry")', 'Text(stringResource(R.string.action_retry))'),
        @('Text("Change server")', 'Text(stringResource(R.string.action_change_server))')
    )
    "SettingsScreen.kt" = @(
        @('contentDescription = "Back"', 'contentDescription = stringResource(R.string.action_back)'),
        @('Text("Save")', 'Text(stringResource(R.string.action_save))'),
        @('SectionLabel("Connection")', 'SectionLabel(stringResource(R.string.settings_section_connection))'),
        @('Text("Your server", style', 'Text(stringResource(R.string.settings_title), style'),
        @('"Use the LAN address on the same Wi-Fi, or the tunnel URL when you are away."', 'stringResource(R.string.settings_hint)'),
        @('{ Text("Server URL") }', '{ Text(stringResource(R.string.settings_server_url)) }'),
        @('{ Text("Password") }', '{ Text(stringResource(R.string.settings_password)) }'),
        @('if (reveal) "Hide password" else "Show password"', 'stringResource(if (reveal) R.string.settings_hide_password else R.string.settings_show_password)'),
        @('Text(if (state == ConnState.Testing) "Testing…" else "Test connection")', 'Text(stringResource(if (state == ConnState.Testing) R.string.action_testing else R.string.action_test))'),
        @('Text(if (canCancel) "Save" else "Connect")', 'Text(stringResource(if (canCancel) R.string.action_save else R.string.action_connect))'),
        @('ConnState.Ok -> "Connected"', 'ConnState.Ok -> stringResource(R.string.settings_connected)'),
        @('ConnState.Failed -> "Not reachable"', 'ConnState.Failed -> stringResource(R.string.settings_not_reachable)'),
        @('ConnState.Testing -> "Checking…"', 'ConnState.Testing -> stringResource(R.string.settings_checking)'),
        @('ConnState.Unknown -> "Not tested yet"', 'ConnState.Unknown -> stringResource(R.string.settings_not_tested)'),
        @('Text("About", style', 'Text(stringResource(R.string.settings_about_title), style'),
        @('"Credentials are stored on this device only.",', 'stringResource(R.string.settings_about_storage),'),
        @('Text("Background notifications", style', 'Text(stringResource(R.string.settings_background_title), style')
    )
    "ChatScreen.kt" = @(
        @('contentDescription = "Back"', 'contentDescription = stringResource(R.string.action_back)'),
        @('contentDescription = "Stop"', 'contentDescription = stringResource(R.string.action_stop)'),
        @('contentDescription = "Settings"', 'contentDescription = stringResource(R.string.action_settings)'),
        @('if (state.busy) "Steer OpenCode…" else "Message OpenCode"', 'stringResource(if (state.busy) R.string.chat_placeholder_busy else R.string.chat_placeholder)'),
        @('contentDescription = "Attach screenshot"', 'contentDescription = stringResource(R.string.chat_attach)'),
        @('contentDescription = "Remove attachment"', 'contentDescription = stringResource(R.string.chat_remove_attachment)'),
        @('contentDescription = "Send"', 'contentDescription = stringResource(R.string.action_send)'),
        @('"Screenshot attached."', 'stringResource(R.string.chat_screenshot_only)'),
        @('Text("Permission requested"', 'Text(stringResource(R.string.permission_title)'),
        @('"OpenCode wants to run an action",', 'stringResource(R.string.permission_subtitle),'),
        @('{ Text("Allow once") }', '{ Text(stringResource(R.string.permission_allow_once)) }'),
        @('{ Text("Always allow") }', '{ Text(stringResource(R.string.permission_always)) }'),
        @('{ Text("Reject", color = MaterialTheme.colorScheme.error) }', '{ Text(stringResource(R.string.permission_reject), color = MaterialTheme.colorScheme.error) }'),
        @('contentDescription = "Attached image"', 'contentDescription = stringResource(R.string.chat_attached_image)')
    )
    "SharePicker.kt" = @(
        @('Text("Send to a session"', 'Text(stringResource(R.string.share_title)'),
        @('if (hasImage) "The image and your note go to the session you choose."', 'if (hasImage) stringResource(R.string.share_body_image)'),
        @('else "Your text goes to the session you choose.",', 'else stringResource(R.string.share_body_text),'),
        @('"No sessions available. Is the server reachable?",', 'stringResource(R.string.share_empty),')
    )
}

foreach ($f in $files.Keys) {
    $text = Get-Content $f -Raw
    foreach ($pair in $files[$f]) {
        $text = $text.Replace($pair[0], $pair[1])
    }
    if ($text -notmatch 'import androidx\.compose\.ui\.res\.stringResource') {
        $text = $text.Replace('import androidx.compose.ui.text.style.TextOverflow', "import androidx.compose.ui.res.stringResource`nimport androidx.compose.ui.text.style.TextOverflow")
    }
    if ($text -notmatch 'import com\.opencode\.mobile\.R') {
        $text = $text.Replace('import com.opencode.mobile.Api', "import com.opencode.mobile.Api`nimport com.opencode.mobile.R")
    }
    Set-Content $f $text -NoNewline
    Write-Output "updated $f"
}
