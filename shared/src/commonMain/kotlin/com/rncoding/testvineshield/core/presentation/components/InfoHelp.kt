package com.rncoding.testvineshield.core.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.rncoding.testvineshield.core.presentation.root.LocalUserActivity
import com.rncoding.testvineshield.core.presentation.root.userAction
import com.rncoding.testvineshield.core.presentation.root.userActivity
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Help stays available independently of a field's editing/saving state. */
@Composable
fun InfoButton(title: String, text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    var open by rememberSaveable { mutableStateOf(false) }
    val activity = LocalUserActivity.current
    IconButton(onClick = userAction { open = true }, modifier = modifier.size(48.dp)) {
        Icon(painterResource(Res.drawable.ic_info), contentDescription = stringResource(Res.string.info_about, title))
    }
    if (open) {
        AlertDialog(
            modifier = Modifier.userActivity(activity),
            onDismissRequest = userAction { open = false },
            title = { Text(title) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    Text(text)
                }
            },
            confirmButton = {
                TextButton(onClick = userAction { open = false }) { Text(stringResource(Res.string.info_close)) }
            }
        )
    }
}

/** The icon sits at the top right of its corresponding visible heading/label. */
@Composable
fun InfoLabel(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleSmall
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(title, Modifier.weight(1f).padding(top = 12.dp), style = style)
        InfoButton(title, text)
    }
}

@Composable
fun InfoHelp(text: String, title: String = stringResource(Res.string.info_general)) {
    InfoLabel(title, text)
}
