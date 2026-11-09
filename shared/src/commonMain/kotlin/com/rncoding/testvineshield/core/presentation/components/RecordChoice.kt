package com.rncoding.testvineshield.core.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rncoding.testvineshield.core.presentation.root.userAction

@Composable
fun <T> RecordChoice(label: String,selected: T?,values: List<T>,enabled: Boolean,text: (T)->String,changed: (T?)->Unit,allowUnknown: Boolean=true,nullLabel: String="Unknown / not specified") {
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) { if(!enabled) open=false }
    Box {
        OutlinedButton(onClick=userAction {open=true},enabled=enabled,modifier=Modifier.fillMaxWidth()) {
            Text("$label: ${selected?.let(text) ?: nullLabel}")
        }
        DropdownMenu(expanded=open && enabled,onDismissRequest=userAction {open=false},modifier=Modifier.heightIn(max=320.dp)) {
            if(allowUnknown) DropdownMenuItem(text={Text(nullLabel)},onClick=userAction {changed(null);open=false})
            values.forEach { value ->
                DropdownMenuItem(text={Text(text(value))},onClick=userAction {changed(value);open=false})
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTopBar(title: String,onBack: (()->Unit)?=null,enabled: Boolean=true) {
    TopAppBar(title={Text(title)},navigationIcon={onBack?.let {back -> TextButton(onClick=userAction(back),enabled=enabled) {Text("Back")} }})
}
