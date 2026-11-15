package com.opencode.mobile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.mobile.MutedStore
import com.opencode.mobile.MutedProjectStore
import com.opencode.mobile.OpenCodeRepository
import com.opencode.mobile.PinnedStore
import com.opencode.mobile.ProjectOption
import com.opencode.mobile.R
import com.opencode.mobile.SeenStore
import com.opencode.mobile.SessionCache
import com.opencode.mobile.SessionItem
import com.opencode.mobile.SessionListViewModel
import com.opencode.mobile.UiPrefs
import com.opencode.mobile.UsageStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SessionGroup { Today, Yesterday, Week, Earlier }

private fun groupOf(millis: Long): SessionGroup {
    if (millis <= 0L) return SessionGroup.Earlier
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(date, LocalDate.now())
    return when {
        days <= 0L -> SessionGroup.Today
        days == 1L -> SessionGroup.Yesterday
        days < 7L -> SessionGroup.Week
        else -> SessionGroup.Earlier
    }
}

private fun labelOf(group: SessionGroup): Int = when (group) {
    SessionGroup.Today -> R.string.sessions_today
    SessionGroup.Yesterday -> R.string.sessions_yesterday
    SessionGroup.Week -> R.string.sessions_week
    SessionGroup.Earlier -> R.string.sessions_earlier
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    repo: OpenCodeRepository,
    onOpen: (SessionItem) -> Unit,
    onSettings: () -> Unit,
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vm: SessionListViewModel = viewModel(key = "sessions") {
        SessionListViewModel(repo, SessionCache(context), context)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val seenStore = remember { SeenStore(context) }
    val showSubagents by UiPrefs.showSubagents.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var projectFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var pinned by remember { mutableStateOf(PinnedStore.all(context)) }
    var muted by remember { mutableStateOf(MutedStore.all(context)) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var expandedParents by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mutedProjects by remember { mutableStateOf(MutedProjectStore.all(context)) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SessionItem?>(null) }
    var deleting by remember { mutableStateOf<SessionItem?>(null) }
    var showCost by remember { mutableStateOf(false) }
    var showMission by remember { mutableStateOf(false) }
    var flashIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var previousRunning by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(state.sessions) {
        val runningNow = state.sessions.filter { it.running }.map { it.id }.toSet()
        val finished = previousRunning - runningNow
        previousRunning = runningNow
        if (finished.isNotEmpty()) {
            flashIds = flashIds + finished
            delay(1400)
            flashIds = flashIds - finished
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val scoped = remember(state.sessions, projectFilter, showArchived) {
        var base = state.sessions.filter { if (showArchived) it.archived else !it.archived }
        base = base.filter { it.parentID.isBlank() }
        if (projectFilter == null) base
        else base.filter { it.projectID == projectFilter }
    }
    val filtered = remember(scoped, query, pinned, state.sessions) {
        val base = if (query.isBlank()) scoped
        else state.sessions.filter { it.title.contains(query, ignoreCase = true) }
        base.sortedWith(
            compareByDescending<SessionItem> { it.id in pinned }
                .thenByDescending { it.updated }
        )
    }
    val childCounts = remember(state.sessions) {
        state.sessions.filter { it.parentID.isNotBlank() }
            .groupingBy { it.parentID }
            .eachCount()
    }
    val childrenByParent = remember(state.sessions) {
        state.sessions.filter { it.parentID.isNotBlank() }.groupBy { it.parentID }
    }
    val running = remember(filtered) { filtered.filter { it.running } }
    val history = remember(filtered) { filtered.filterNot { it.running } }
    // The saved scroll offset reopens the list mid-history. If anything is
    // running, its cards are at the top and would be invisible.
    var initialScrollSettled by remember { mutableStateOf(false) }
    LaunchedEffect(running.isNotEmpty(), state.loading) {
        if (initialScrollSettled) return@LaunchedEffect
        if (state.loading && state.sessions.isEmpty()) return@LaunchedEffect
        initialScrollSettled = true
        if (running.isNotEmpty()) {
            runCatching { listState.scrollToItem(0) }
        }
    }
    val grouped = remember(history) { history.groupBy { groupOf(it.updated) } }
    val projectNames = remember(state.projects) {
        state.projects.associate { it.id to it.name }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searchOpen) {
        if (searchOpen) runCatching { focusRequester.requestFocus() }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Compact bar: title and actions, or an expanding search field.
        AnimatedContent(
            targetState = searchOpen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "topbar"
        ) { searching ->
            if (searching) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.sessions_search)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = {
                        searchOpen = false
                        query = ""
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        val greeting = remember {
                            when (java.time.LocalTime.now().hour) {
                                in 5..11 -> R.string.sessions_greeting_morning
                                in 12..17 -> R.string.sessions_greeting_afternoon
                                else -> R.string.sessions_greeting_evening
                            }
                        }
                        val eyebrow = when {
                            state.error != null -> stringResource(R.string.sessions_reconnecting_short)
                            else -> buildString {
                                append(stringResource(greeting))
                                append(" · ")
                                append(stringResource(R.string.sessions_count, filtered.size))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                eyebrow,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.error != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (running.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Row(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .clickable {
                                            scope.launch {
                                                runCatching { listState.animateScrollToItem(0) }
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${running.size} ${stringResource(R.string.session_running)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Rounded.ArrowUpward,
                                        contentDescription = stringResource(R.string.sessions_jump_live),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.cost_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showCost = true }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            stringResource(R.string.sessions_title),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = { searchOpen = true }) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.sessions_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        onClick = onNewSession,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.action_new_session),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        val archivedCount = state.sessions.count { it.archived }
        val subagentCount = state.sessions.count { it.parentID.isNotBlank() }
        if (state.projects.size > 1 || archivedCount > 0 || subagentCount > 0) {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.projects.size > 1) {
                    item {
                        ProjectChip(
                            label = stringResource(R.string.sessions_all_projects),
                            count = filtered.size,
                            selected = projectFilter == null,
                            onClick = { projectFilter = null }
                        )
                    }
                    items(
                        state.projects.filter { it.name.isNotBlank() || it.id.isNotBlank() },
                        key = { it.id }
                    ) { project ->
                        ProjectChip(
                            label = projectLabel(project, state.sessions),
                            count = state.sessions.count { it.projectID == project.id },
                            selected = projectFilter == project.id,
                            onClick = {
                                projectFilter = if (projectFilter == project.id) null else project.id
                            }
                        )
                    }
                }
                if (archivedCount > 0) {
                    item {
                        ProjectChip(
                            label = stringResource(R.string.sessions_archived),
                            count = archivedCount,
                            selected = showArchived,
                            onClick = { showArchived = !showArchived }
                        )
                    }
                }
            }
        }

        // The list and its floating create button share one box so the button
        // is positioned inside the layout instead of relying on draw overflow.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            when {
                state.loading && state.sessions.isEmpty() -> Column(Modifier.fillMaxWidth()) {
                    repeat(6) { SessionSkeleton() }
                }

            state.sessions.isEmpty() -> EmptySessions(
                error = state.error,
                onRetry = { vm.refresh() },
                onSettings = onSettings,
                onNewSession = onNewSession
            )

            filtered.isEmpty() -> Text(
                stringResource(R.string.sessions_no_match),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp)
            )

            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { vm.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                if (running.isNotEmpty()) {
                    item(key = "active-strip") {
                        ActiveCards(
                            running = running,
                            previews = state.previews,
                            childCounts = if (showSubagents) childCounts else emptyMap(),
                            onOpen = onOpen,
                            onRename = { renaming = it },
                            onDelete = { deleting = it },
                            onMissionControl = { showMission = true }
                        )
                    }
                }

                SessionGroup.entries.forEach { group ->
                    val sessions = grouped[group].orEmpty()
                    if (sessions.isEmpty()) return@forEach

                    stickyHeader(key = "header-$group") {
                        Text(
                            stringResource(labelOf(group)).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(start = 20.dp, top = 18.dp, bottom = 6.dp)
                        )
                    }

                    sessions.forEach { session ->
                        item(key = session.id) {
                        Box(Modifier.animateItem()) {
                            val seen = seenStore.lastSeen(session.id)
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        deleting = session
                                    }
                                    false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp, vertical = 3.dp)
                                            .background(
                                                MaterialTheme.colorScheme.errorContainer,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            stringResource(R.string.action_delete),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            ) {
                                SessionRow(
                                    session = session,
                                    preview = state.previews[session.id],
                                    unread = seen > 0L && session.updated > seen,
                                    pinned = session.id in pinned,
                                    muted = session.id in muted,
                                    archived = session.archived,
                                    projectMuted = session.projectID in mutedProjects,
                                    projectName = if (projectFilter == null) {
                                        projectNames[session.projectID].orEmpty()
                                    } else {
                                        ""
                                    },
                                    subagents = if (showSubagents) {
                                        childCounts[session.id] ?: 0
                                    } else {
                                        0
                                    },
                                    subagentsExpanded = session.id in expandedParents,
                                    justFinished = session.id in flashIds,
                                    onToggleSubagents = if (
                                        showSubagents &&
                                        (childCounts[session.id] ?: 0) > 0
                                    ) {
                                        {
                                            expandedParents =
                                                if (session.id in expandedParents) {
                                                    expandedParents - session.id
                                                } else {
                                                    expandedParents + session.id
                                                }
                                        }
                                    } else {
                                        null
                                    },
                                    onOpen = { onOpen(session) },
                                    onRename = { renaming = session },
                                    onDelete = { deleting = session },
                                    onTogglePin = {
                                        pinned = PinnedStore.toggle(context, session.id)
                                    },
                                    onToggleMute = {
                                        muted = MutedStore.toggle(context, session.id)
                                    },
                                    onArchive = { vm.archive(session.id, !session.archived) },
                                    onToggleProjectMute = {
                                        if (session.projectID.isNotBlank()) {
                                            mutedProjects = MutedProjectStore.toggle(
                                                context,
                                                session.projectID
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        }
                        val children = childrenByParent[session.id].orEmpty()
                        if (showSubagents && children.isNotEmpty()) {
                            if (session.id in expandedParents) {
                                children.forEach { child ->
                                    val childSeen = seenStore.lastSeen(child.id)
                                    item(key = child.id) {
                                        Box(Modifier.animateItem().padding(start = 24.dp)) {
                                            SessionRow(
                                                session = child,
                                                preview = state.previews[child.id],
                                                unread = childSeen > 0L && child.updated > childSeen,
                                                pinned = child.id in pinned,
                                                muted = child.id in muted,
                                                archived = child.archived,
                                                projectMuted = child.projectID in mutedProjects,
                                                subagents = 0,
                                                subagent = true,
                                                justFinished = child.id in flashIds,
                                                onOpen = { onOpen(child) },
                                                onRename = { renaming = child },
                                                onDelete = { deleting = child },
                                                onTogglePin = {
                                                    pinned = PinnedStore.toggle(context, child.id)
                                                },
                                                onToggleMute = {
                                                    muted = MutedStore.toggle(context, child.id)
                                                },
                                                onArchive = {
                                                    vm.archive(child.id, !child.archived)
                                                },
                                                onToggleProjectMute = {
                                                    if (child.projectID.isNotBlank()) {
                                                        mutedProjects = MutedProjectStore.toggle(
                                                            context,
                                                            child.projectID
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    }

}


    if (showCost) {
        CostSheet(sessions = state.sessions, onDismiss = { showCost = false })
    }

    if (showMission) {
        MissionControlSheet(
            running = running,
            previews = state.previews,
            onOpen = { session ->
                showMission = false
                onOpen(session)
            },
            onStop = { session -> vm.stop(session.id) },
            onDismiss = { showMission = false }
        )
    }

    renaming?.let { session ->
        RenameDialog(
            initial = session.title,
            onDismiss = { renaming = null },
            onConfirm = { title ->
                vm.rename(session.id, title)
                renaming = null
            }
        )
    }

    deleting?.let { session ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.session_delete_title)) },
            text = { Text(stringResource(R.string.session_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(session.id)
                    deleting = null
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ProjectChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent
                else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.width(6.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = (if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)
        )
    }
}

/** Project ids without a name fall back to the folder name, never "Project 76d3". */
@Composable
private fun projectLabel(project: ProjectOption, sessions: List<SessionItem>): String {
    if (project.name.isNotBlank()) return project.name
    val base = sessions.firstOrNull { it.projectID == project.id }
        ?.directory
        .orEmpty()
        .trimEnd('\\', '/')
        .substringAfterLast('\\')
        .substringAfterLast('/')
    return base.ifBlank {
        stringResource(R.string.sessions_project_fallback, project.id.take(4))
    }
}

/**
 * Live work is state, not history. Vertical, capped at two cards, with an
 * explicit "show all" for the rest — no horizontal scrolling for status.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ActiveCards(
    running: List<SessionItem>,
    previews: Map<String, String>,
    childCounts: Map<String, Int>,
    onOpen: (SessionItem) -> Unit,
    onRename: (SessionItem) -> Unit,
    onDelete: (SessionItem) -> Unit,
    onMissionControl: () -> Unit
) {
    var showAll by remember { mutableStateOf(false) }
    val visible = running.take(2)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.session_running).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.sessions_mission_control),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onMissionControl)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        visible.forEach { session ->
            ActiveCard(
                session = session,
                preview = previews[session.id],
                subagents = childCounts[session.id] ?: 0,
                onClick = { onOpen(session) },
                onRename = { onRename(session) },
                onDelete = { onDelete(session) }
            )
        }

        if (running.size > visible.size) {
            Surface(
                onClick = { showAll = true },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.sessions_more_running, running.size - visible.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.sessions_show_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showAll) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAll = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(
                    stringResource(R.string.sessions_all_running),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )
                running.forEach { session ->
                    ActiveCard(
                        session = session,
                        preview = previews[session.id],
                        subagents = childCounts[session.id] ?: 0,
                        onClick = {
                            showAll = false
                            onOpen(session)
                        },
                        onRename = {
                            showAll = false
                            onRename(session)
                        },
                        onDelete = {
                            showAll = false
                            onDelete(session)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ActiveCard(
    session: SessionItem,
    preview: String?,
    subagents: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val elapsed = elapsedClock(session.busyAt, now)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActivityBars(color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        session.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (subagents > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (subagents > 9) "9+" else "$subagents",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
                Text(
                    buildString {
                        append(stringResource(R.string.chat_activity_running_short, elapsed))
                        val line = preview?.let { cleanPreview(it) }
                        if (!line.isNullOrBlank()) append(" · ").append(line)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.session_more),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    shape = RoundedCornerShape(14.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(
    session: SessionItem,
    preview: String?,
    unread: Boolean,
    pinned: Boolean,
    muted: Boolean,
    archived: Boolean,
    projectMuted: Boolean,
    subagents: Int,
    projectName: String = "",
    subagent: Boolean = false,
    subagentsExpanded: Boolean = false,
    justFinished: Boolean = false,
    onToggleSubagents: (() -> Unit)? = null,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onArchive: () -> Unit,
    onToggleProjectMute: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val cleanPreview = remember(preview) { cleanPreview(preview) }
    val meta = remember(projectName, cleanPreview) {
        listOfNotNull(
            projectName.takeIf { it.isNotBlank() },
            cleanPreview
        ).joinToString(" · ")
    }

    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sessionRowPress"
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (justFinished) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        animationSpec = androidx.compose.animation.core.tween(260),
        label = "sessionRowBorder"
    )
    Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .clip(shape)
                .background(
                    if (androidx.compose.foundation.isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
                .border(
                    width = if (justFinished) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = shape
                )
                .combinedClickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onOpen,
                    onLongClick = { menuOpen = true }
                )
                .padding(start = 12.dp, end = 4.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reserved gutter: dots never shift titles out of alignment.
            Box(Modifier.width(10.dp).height(30.dp), contentAlignment = Alignment.CenterStart) {
                when {
                    session.running -> PulsingDot(
                        color = MaterialTheme.colorScheme.secondary,
                        pulsing = true,
                        size = 6
                    )

                    unread -> Box(
                        Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        session.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (subagent) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.session_subagent_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (pinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = stringResource(R.string.action_pin),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (muted) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.NotificationsOff,
                            contentDescription = stringResource(R.string.action_unmute),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (archived) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.sessions_archived),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (meta.isNotBlank() || subagents > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (subagents > 0) {
                            Spacer(Modifier.width(6.dp))
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = onToggleSubagents != null) {
                                        onToggleSubagents?.invoke()
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (subagents > 9) "9+" else "$subagents",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (onToggleSubagents != null) {
                                    Icon(
                                        if (subagentsExpanded) Icons.Rounded.ExpandLess
                                        else Icons.Rounded.ExpandMore,
                                        contentDescription = stringResource(
                                            if (subagentsExpanded) R.string.sessions_hide_subagents
                                            else R.string.sessions_show_subagents,
                                            subagents
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                relativeTime(session.updated),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 62.dp)
            )
            // Long-press still works, but the menu needs a visible affordance.
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.session_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Anchor the menu to a zero-size box at the row's trailing edge; Material
        // positions the popup from its parent, so align() on the menu alone puts
        // it at the row's start.
        Box(Modifier.align(Alignment.TopEnd)) {
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                onClick = {
                    menuOpen = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (pinned) R.string.action_unpin else R.string.action_pin
                        )
                    )
                },
                onClick = {
                    menuOpen = false
                    onTogglePin()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (muted) R.string.action_unmute else R.string.action_mute
                        )
                    )
                },
                onClick = {
                    menuOpen = false
                    onToggleMute()
                }
            )
            if (session.projectID.isNotBlank()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (projectMuted) R.string.action_unmute_project
                                else R.string.action_mute_project
                            )
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onToggleProjectMute()
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (archived) R.string.action_unarchive else R.string.action_archive
                        )
                    )
                },
                onClick = {
                    menuOpen = false
                    onArchive()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_copy_id)) },
                onClick = {
                    menuOpen = false
                    clipboard.setText(AnnotatedString(session.id))
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    menuOpen = false
                    onDelete()
                }
            )
            }
        }
    }
}

/** Drops absolute paths and long command dumps from the one-line preview. */
private fun cleanPreview(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    var value = raw
        .replace(Regex("[A-Za-z]:\\\\[^\\s]+"), "…")
        .replace(Regex("(?:/[\\w.-]+){3,}"), "…")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (value.length > 110) value = value.take(107).trimEnd() + "…"
    return value.ifBlank { null }
}

/**
 * The live board: every running agent as a tile with its clock, current line,
 * and a stop control.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MissionControlSheet(
    running: List<SessionItem>,
    previews: Map<String, String>,
    onOpen: (SessionItem) -> Unit,
    onStop: (SessionItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.sessions_mission_control),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "${running.size} ${stringResource(R.string.session_running)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_cancel)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                running.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { session ->
                            MissionTile(
                                session = session,
                                preview = previews[session.id],
                                now = now,
                                onOpen = { onOpen(session) },
                                onStop = { onStop(session) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionTile(
    session: SessionItem,
    preview: String?,
    now: Long,
    onOpen: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActivityBars(color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(7.dp))
                Text(
                    elapsedClock(session.busyAt, now),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onStop, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Rounded.Stop,
                        contentDescription = stringResource(R.string.action_stop),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                session.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val line = preview?.let { cleanPreview(it) }
            if (!line.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Live `10m 07s` clock for a running session. */
private fun elapsedClock(since: Long, now: Long): String {
    if (since <= 0L || now <= since) return "0s"
    val total = (now - since) / 1000L
    return when {
        total < 60L -> "${total}s"
        total < 3600L -> "${total / 60L}m ${(total % 60L).toString().padStart(2, '0')}s"
        else -> "${total / 3600L}h ${((total % 3600L) / 60L).toString().padStart(2, '0')}m"
    }
}

/** Spend across sessions: total, by model, and the most expensive sessions. */@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CostSheet(sessions: List<SessionItem>, onDismiss: () -> Unit) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val total = sessions.sumOf { it.cost }
    val byModel = sessions.filter { it.cost > 0 }
        .groupBy { s -> if (s.modelID.isBlank()) "—" else "${s.modelProviderID}/${s.modelID}" }
        .map { (name, list) -> name to list.sumOf { it.cost } }
        .sortedByDescending { it.second }
    val top = sessions.filter { it.cost > 0 }.sortedByDescending { it.cost }.take(8)
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = remember { UsageStore.today(context) }
    val week = remember { UsageStore.recent(context, 7) }
    val weekTotal = week.sumOf { it.cost }
    val month = remember { UsageStore.monthToDate(context) }
    val projected = remember { UsageStore.projectedMonth(context) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.cost_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.cost_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatCost(total),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.cost_today),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatCost(today),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.cost_week),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatCost(weekTotal),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    week.forEach { day ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                day.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp)
                            )
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = {
                                    (day.cost / maxOf(0.01, week.maxOfOrNull { it.cost } ?: 0.01))
                                        .toFloat().coerceIn(0f, 1f)
                                },
                                modifier = Modifier.weight(1f).height(5.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            Text(
                                formatCost(day.cost),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(66.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(
                            R.string.cost_month,
                            formatCost(month),
                            formatCost(projected)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val exportLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.CreateDocument("text/csv")
                    ) { uri ->
                        if (uri != null) {
                            runCatching {
                                context.contentResolver.openOutputStream(uri)?.use { stream ->
                                    stream.write(UsageStore.csv(context).toByteArray())
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        exportLauncher.launch("opencode-usage.csv")
                    }) {
                        Text(stringResource(R.string.cost_export))
                    }
                }
            }

            if (byModel.isEmpty()) {
                Text(
                    stringResource(R.string.cost_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.cost_by_model),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                byModel.forEach { (name, cost) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatCost(cost),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    stringResource(R.string.cost_top_sessions),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp)
                )
                top.forEach { session ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            session.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatCost(session.cost),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_rename_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun EmptySessions(
    error: String?,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onNewSession: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            ActivityBars(color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            if (error != null) stringResource(R.string.sessions_unreachable_title)
            else stringResource(R.string.sessions_empty_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            error ?: stringResource(R.string.sessions_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        if (error != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                TextButton(onClick = onSettings) { Text(stringResource(R.string.action_change_server)) }
            }
        } else {
            Button(
                onClick = onNewSession,
                shape = RoundedCornerShape(50)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_new_session))
            }
        }
    }
}
