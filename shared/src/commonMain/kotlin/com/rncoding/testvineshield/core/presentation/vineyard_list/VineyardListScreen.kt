package com.rncoding.testvineshield.core.presentation.vineyard_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
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
import androidx.compose.material3.SnackbarHost
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
                    Text("Vineyards")
                },
                actions = {
                    TextButton(onClick = userAction(onOpenDiseaseCatalogue)) { Text("Diseases") }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = userAction(onCreateVineyard)
            ) {
                Text("+")
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

            state.errorMessage != null &&
                    state.vineyards.isEmpty() -> {
                VineyardListError(
                    message = state.errorMessage,
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
                                    if (!current.isReordering && current.vineyardPendingDeletion == null) {
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
                                    if (id != null && from >= 0 && to >= 0 && from != to) {
                                        reorder(id, to - from)
                                    }
                                }
                            )
                        },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isReordering) {
                        item(key = "saving-vineyard-order") {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                    itemsIndexed(
                        items = state.vineyards,
                        key = { _, vineyard ->
                            vineyard.vineyardId
                        }
                    ) { index, vineyard ->
                        DisposableEffect(vineyard.vineyardId) {
                            onDispose { handleBounds.remove(vineyard.vineyardId) }
                        }
                        VineyardListItem(
                            vineyard = vineyard,
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
                            onMoveUp = if (!state.isReordering && index > 0) {
                                { onReorder(vineyard.vineyardId, -1) }
                            } else null,
                            onMoveDown = if (!state.isReordering && index < state.vineyards.lastIndex) {
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
                Text("Delete vineyard?")
            },
            text = {
                Text(
                    "This will permanently delete the vineyard " +
                            "and its related local data."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = userAction(onDeleteConfirm)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = userAction(onDeleteCancel)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VineyardListItem(
    vineyard: VineyardListItemUiModel,
    dragModifier: Modifier,
    dropTarget: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
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
                    Text("Delete")
                }
            }

            Text(
                if (dropTarget) "Drop here" else "Hold to reorder",
                modifier = dragModifier
                    .semantics { contentDescription = "Hold and drag to reorder ${vineyard.name}" }
                    .padding(12.dp)
            )
            Row {
                TextButton(
                    onClick = userAction { onMoveUp?.invoke() },
                    enabled = onMoveUp != null
                ) { Text("Move up") }
                TextButton(
                    onClick = userAction { onMoveDown?.invoke() },
                    enabled = onMoveDown != null
                ) { Text("Move down") }
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
                text = when (vineyard.blockCount) {
                    1 -> "1 block"
                    else -> "${vineyard.blockCount} blocks"
                }
            )

            if (vineyard.latestTemperature != null) {
                Text("Cached weather saved · open vineyard for timestamps")
            }

            vineyard.lastActivityStatus?.let { status ->
                val priority =
                    vineyard.lastActivityPriority
                        ?.takeIf { it.isNotBlank() }

                Text(
                    text = if (priority != null) {
                        "Last activity: $status · $priority"
                    } else {
                        "Last activity: $status"
                    }
                )
            }

            vineyard.activeAlert?.let { alert ->
                if (alert.isNotBlank()) {
                    Text(
                        text = "Alert: $alert"
                    )
                }
            }

            if (vineyard.currentDiseaseCount > 0) {
                Text(
                    text =
                        "Active diseases: " +
                                vineyard.currentDiseaseCount
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
                text = "No vineyards yet."
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = userAction(onCreateVineyard)
            ) {
                Text("Create vineyard")
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