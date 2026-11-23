package com.rncoding.testvineshield.core.presentation.vineyard_list

import com.rncoding.testvineshield.core.presentation.vineyards.VineyardLoadIssue
import com.rncoding.testvineshield.core.presentation.vineyards.localizedVineyardLoadIssue

import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rncoding.testvineshield.core.presentation.root.LocalUserActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import com.rncoding.testvineshield.core.presentation.error.LocalizedErrorSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rncoding.testvineshield.core.presentation.root.userAction
import com.rncoding.testvineshield.core.presentation.root.userActivity
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VineyardListScreen(
    state: VineyardListUiState,
    snackbarHostState: SnackbarHostState,
    onVineyardClick: (Long) -> Unit,
    onCreateVineyard: () -> Unit,
    onOpenDiseaseCatalogue: () -> Unit,
    onDeleteRequest: (Long) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit,
    onReorder: (Long, Int) -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val searchQuery = searchText.trim()
    val searchActive = searchQuery.isNotEmpty()
    val visibleVineyards = remember(state.vineyards, searchQuery) {
        state.vineyards.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.location.contains(searchQuery, ignoreCase = true)
        }
    }
    val latestSearchActive by rememberUpdatedState(searchActive)
    val createDescription = stringResource(Res.string.create_vineyard)
    val listState = rememberLazyListState()
    val latestState by rememberUpdatedState(state)
    val reorder by rememberUpdatedState(onReorder)
    val activity = LocalUserActivity.current
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var targetId by remember { mutableStateOf<Long?>(null) }
    var dragY by remember { mutableStateOf(0f) }

    val handleBounds = remember { mutableMapOf<Long, Rect>() }
    var listBounds by remember { mutableStateOf(Rect.Zero) }
    val edgeSize = with(LocalDensity.current) { 56.dp.toPx() }
    val maximumSpeed = with(LocalDensity.current) { 480.dp.toPx() }
    val currentActivity by rememberUpdatedState(activity)

    fun updateDropTarget() {
        val layout = listState.layoutInfo
        val y = dragY.coerceIn(
            layout.viewportStartOffset.toFloat(),
            (layout.viewportEndOffset - 1).coerceAtLeast(layout.viewportStartOffset).toFloat()
        )
        val target = layout.visibleItemsInfo.firstOrNull { item ->
            y >= item.offset && y < item.offset + item.size
        } ?: layout.visibleItemsInfo.minByOrNull { item ->
            kotlin.math.abs(y - (item.offset + item.size / 2f))
        }
        target?.key?.let { key ->
            latestState.vineyards.firstOrNull { it.vineyardId == key }
                ?.let { targetId = it.vineyardId }
        }
    }

    LaunchedEffect(draggedId) {
        if (draggedId == null) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (draggedId != null) {
            val frame = withFrameNanos { it }
            val elapsed = ((frame - previousFrame) / 1_000_000_000f).coerceAtMost(0.05f)
            previousFrame = frame
            val layout = listState.layoutInfo
            val top = layout.viewportStartOffset.toFloat()
            val bottom = layout.viewportEndOffset.toFloat()
            val speed = when {
                dragY < top + edgeSize ->
                    -maximumSpeed * ((top + edgeSize - dragY) / edgeSize).coerceIn(0f, 1f)
                dragY > bottom - edgeSize ->
                    maximumSpeed * ((dragY - bottom + edgeSize) / edgeSize).coerceIn(0f, 1f)
                else -> 0f
            }
            if (speed != 0f) {
                listState.scrollBy(speed * elapsed)
                currentActivity()
            }
            updateDropTarget()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.nav_vineyards))
                },
                actions = {
                    TextButton(onClick = userAction(onOpenDiseaseCatalogue)) { Text(stringResource(Res.string.vineyard_diseases)) }
                }
            )
        },
        snackbarHost = {
            LocalizedErrorSnackbarHost(
                hostState = snackbarHostState
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = userAction(onCreateVineyard)
            ) {
                Text("+", Modifier.semantics { contentDescription = createDescription })
            }
        }
    ) { innerPadding ->

        when {
            state.isLoading -> {
                VineyardListLoading(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            state.loadIssue != null &&
                    state.vineyards.isEmpty() -> {
                VineyardListError(
                    message = localizedVineyardLoadIssue(state.loadIssue),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            state.vineyards.isEmpty() -> {
                EmptyVineyardList(
                    onCreateVineyard = onCreateVineyard,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    userScrollEnabled = draggedId == null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .onGloballyPositioned { listBounds = it.boundsInRoot() }
                        .pointerInput(state.isReordering) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val current = latestState
                                    if (!latestSearchActive && !current.isReordering && current.vineyardPendingDeletion == null) {
                                        val rootPoint = offset + listBounds.topLeft
                                        val id = handleBounds.entries.firstOrNull { it.value.contains(rootPoint) }?.key
                                        if (id != null && current.vineyards.any { it.vineyardId == id }) {
                                            currentActivity()
                                            draggedId = id
                                            targetId = id
                                            dragY = offset.y
                                        }
                                    }
                                },
                                onDrag = { change, delta ->
                                    if (draggedId != null) {
                                        change.consume()
                                        currentActivity()
                                        dragY += delta.y
                                        updateDropTarget()
                                    }
                                },
                                onDragCancel = {
                                    draggedId = null
                                    targetId = null
                                },
                                onDragEnd = {
                                    val id = draggedId
                                    val ids = latestState.vineyards.map { it.vineyardId }
                                    val from = ids.indexOf(id)
                                    val to = ids.indexOf(targetId)
                                    draggedId = null
                                    targetId = null
                                    if (!latestSearchActive && id != null && from >= 0 && to >= 0 && from != to) {
                                        reorder(id, to - from)
                                    }
                                }
                            )
                        },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "vineyard-search") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { activity(); searchText = it },
                                label = { Text(stringResource(Res.string.vineyard_search)) },
                                enabled = !state.isReordering && draggedId == null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (searchText.isNotEmpty()) {
                                TextButton(
                                    onClick = userAction { searchText = "" },
                                    enabled = !state.isReordering && draggedId == null
                                ) { Text(stringResource(Res.string.clear_search)) }
                            }
                            if (searchActive) Text(stringResource(Res.string.reorder_clear_search))
                            if (visibleVineyards.isEmpty()) Text(stringResource(Res.string.vineyard_no_matches))
                        }
                    }
                    if (state.isReordering) {
                        item(key = "saving-vineyard-order") {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                    itemsIndexed(
                        items = visibleVineyards,
                        key = { _, vineyard ->
                            vineyard.vineyardId
                        }
                    ) { index, vineyard ->
                        DisposableEffect(vineyard.vineyardId) {
                            onDispose { handleBounds.remove(vineyard.vineyardId) }
                        }
                        VineyardListItem(
                            vineyard = vineyard,
                            reorderAvailable = !searchActive,
                            dragModifier = Modifier.onGloballyPositioned { coordinates ->
                                handleBounds[vineyard.vineyardId] = coordinates.boundsInRoot()
                            },
                            dropTarget = draggedId != null && targetId == vineyard.vineyardId,
                            onClick = userAction {
                                onVineyardClick(
                                    vineyard.vineyardId
                                )
                            },
                            onDelete = if (state.isReordering) null else {
                                { onDeleteRequest(vineyard.vineyardId) }
                            },
                            onMoveUp = if (!searchActive && !state.isReordering && index > 0) {
                                { onReorder(vineyard.vineyardId, -1) }
                            } else null,
                            onMoveDown = if (!searchActive && !state.isReordering && index < state.vineyards.lastIndex) {
                                { onReorder(vineyard.vineyardId, 1) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    if (state.vineyardPendingDeletion != null) {
        AlertDialog(
            modifier = Modifier.userActivity(com.rncoding.testvineshield.core.presentation.root.LocalUserActivity.current),
            onDismissRequest = userAction(onDeleteCancel),
            title = {
                Text(stringResource(Res.string.delete_vineyard_title))
            },
            text = {
                Text(
                    stringResource(Res.string.delete_vineyard_message)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = userAction(onDeleteConfirm)
                ) {
                    Text(stringResource(Res.string.delete_record))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = userAction(onDeleteCancel)
                ) {
                    Text(stringResource(Res.string.cancel_action))
                }
            }
        )
    }
}

@Composable
private fun VineyardListItem(
    vineyard: VineyardListItemUiModel,
    reorderAvailable: Boolean,
    dragModifier: Modifier,
    dropTarget: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val dragDescription = stringResource(Res.string.drag_vineyard, vineyard.name)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = userAction(onClick)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text = vineyard.name
                )

                TextButton(
                    onClick = userAction { onDelete?.invoke() },
                    enabled = onDelete != null
                ) {
                    Text(stringResource(Res.string.delete_record))
                }
            }

            if (reorderAvailable) {
                Text(
                    if (dropTarget) stringResource(Res.string.drop_here) else stringResource(Res.string.hold_reorder),
                    modifier = dragModifier
                        .semantics { contentDescription = dragDescription }
                        .padding(12.dp)
                )
                Row {
                    TextButton(
                        onClick = userAction { onMoveUp?.invoke() },
                        enabled = onMoveUp != null
                    ) { Text(stringResource(Res.string.move_up)) }
                    TextButton(
                        onClick = userAction { onMoveDown?.invoke() },
                        enabled = onMoveDown != null
                    ) { Text(stringResource(Res.string.move_down)) }
                }

            }

            if (vineyard.location.isNotBlank()) {
                Text(
                    text = vineyard.location
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = stringResource(Res.string.block_count, vineyard.blockCount)
            )

            if (vineyard.latestTemperature != null) {
                Text(stringResource(Res.string.cached_weather_note))
            }

            vineyard.lastActivityStatus?.let { status ->
                val priority =
                    vineyard.lastActivityPriority
                        ?.takeIf { it.isNotBlank() }

                Text(
                    text = if (priority != null) {
                        stringResource(Res.string.last_activity_priority, localizedStatus(status), localizedPriority(priority))
                    } else {
                        stringResource(Res.string.last_activity, localizedStatus(status))
                    }
                )
            }

            vineyard.activeAlert?.let { alert ->
                if (alert.isNotBlank()) {
                    Text(
                        text = stringResource(Res.string.alert_summary, alert)
                    )
                }
            }

            if (vineyard.currentDiseaseCount > 0) {
                Text(
                    text =
                        stringResource(Res.string.active_disease_count, vineyard.currentDiseaseCount)
                )
            }
        }
    }
}

@Composable
private fun VineyardListLoading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyVineyardList(
    onCreateVineyard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.no_vineyards_yet)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = userAction(onCreateVineyard)
            ) {
                Text(stringResource(Res.string.create_vineyard))
            }
        }
    }
}

@Composable
private fun VineyardListError(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message
        )
    }
}
@Composable
private fun localizedStatus(value: String): String = when (value) {
    "planned" -> stringResource(Res.string.status_planned)
    "in_progress" -> stringResource(Res.string.status_progress)
    "completed" -> stringResource(Res.string.status_completed)
    "cancelled" -> stringResource(Res.string.status_cancelled)
    else -> value
}

@Composable
private fun localizedPriority(value: String): String = when (value) {
    "low" -> stringResource(Res.string.priority_low)
    "medium" -> stringResource(Res.string.priority_medium)
    "high" -> stringResource(Res.string.priority_high)
    "urgent" -> stringResource(Res.string.priority_urgent)
    else -> value
}
