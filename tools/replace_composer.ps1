$ErrorActionPreference = "Stop"
$path = "C:\Workspace\opencode-mobile\app\src\main\java\com\opencode\mobile\ui\ChatScreen.kt"
$text = Get-Content $path -Raw

$pattern = '(?s)            Surface\(\r?\n                color = MaterialTheme\.colorScheme\.surfaceContainerHigh,.*?\r?\n    \}\r?\n\r?\n    state\.pendingPermission'

$replacement = @'
            Composer(
                value = input,
                onValueChange = { input = it },
                attachments = picked.map { AttachmentPreview(it.uri, it.attachment.name) },
                busy = state.busy,
                onAttach = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemove = { preview ->
                    picked = picked.filterNot { it.uri == preview.uri }
                },
                onSend = {
                    val text = input.trim()
                    val files = picked.map { it.attachment }
                    if (text.isNotEmpty() || files.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        input = ""
                        picked = emptyList()
                        vm.send(text.ifBlank { screenshotOnlyText }, files) { ok ->
                            if (!ok) input = text
                        }
                    }
                },
                onStop = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.interrupt()
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    state.pendingPermission
'@

if ($text -notmatch $pattern) { throw "composer block not found" }
$text = [regex]::Replace($text, $pattern, $replacement)
Set-Content $path $text -NoNewline
Write-Output "composer replaced"
