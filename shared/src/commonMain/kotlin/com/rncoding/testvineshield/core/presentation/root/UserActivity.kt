package com.rncoding.testvineshield.core.presentation.root

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Call from editable widgets as IME text changes need not emit hardware key events. */
val LocalUserActivity = staticCompositionLocalOf<() -> Unit> { {} }

fun Modifier.userActivity(onActivity: () -> Unit): Modifier =
    onPreviewKeyEvent { onActivity(); false }.pointerInput(onActivity) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.pressed }) onActivity()
            }
        }
    }

/** Button/semantics actions also count when invoked by accessibility without a pointer event. */
@androidx.compose.runtime.Composable
fun userAction(action: () -> Unit): () -> Unit {
    val activity = LocalUserActivity.current
    return { activity(); action() }
}
