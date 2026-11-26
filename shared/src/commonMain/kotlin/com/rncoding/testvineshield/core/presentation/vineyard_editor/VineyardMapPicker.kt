package com.rncoding.testvineshield.core.presentation.vineyard_editor

import com.rncoding.testvineshield.core.presentation.components.InfoHelp
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rncoding.testvineshield.core.domain.datamodels.VineyardMapPoint
import com.rncoding.testvineshield.core.domain.repository.LocationMetadataRepository
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.presentation.root.*
import com.rncoding.testvineshield.core.presentation.formatting.formatPlainDecimal
import kotlinx.coroutines.*
import org.koin.compose.koinInject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/** Native map is composed only after opening this explicit on-demand dialog. */
@Composable
fun VineyardMapPicker(initial: VineyardMapPoint?,onDismiss: ()->Unit,onUse: (VineyardMapPoint,String?)->Unit) {
    val activity=LocalUserActivity.current
    val metadata=koinInject<LocationMetadataRepository>()
    val scope=rememberCoroutineScope()
    val uri=LocalUriHandler.current
    val state=rememberMapState(baseStyle=BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        initialCameraPosition=CameraPosition(center=Position(latitude=(initial?.latitude ?: 42.0).coerceIn(-85.0,85.0),longitude=initial?.longitude ?: 24.0),zoom=if(initial==null) 3.0 else 14.0))
    val point=VineyardMapPoint.from(state.cameraPosition.center.latitude,state.cameraPosition.center.longitude)
    val currentPoint by rememberUpdatedState(point)
    var revision by remember { mutableLongStateOf(0L) }
    var zonePoint by remember { mutableStateOf<VineyardMapPoint?>(null) }
    var zone by remember { mutableStateOf<String?>(null) }
    var useZone by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var lookupError by remember { mutableStateOf<AppError?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(point) { revision++; job?.cancel(); zonePoint=null; zone=null; useZone=false; busy=false; lookupError=null }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().userActivity(activity)) {
            Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.map_choose_location),style=MaterialTheme.typography.titleLarge)
                InfoHelp(title = stringResource(Res.string.info_map), text = stringResource(Res.string.map_privacy))
                InfoHelp(title = stringResource(Res.string.map_choose_location), text = stringResource(Res.string.map_position_help))
                Box(Modifier.weight(1f).fillMaxWidth().heightIn(min=180.dp)) {
                    MaplibreMap(state=state,modifier=Modifier.fillMaxSize().userActivity(activity))
                    val color=MaterialTheme.colorScheme.error
                    Canvas(Modifier.fillMaxSize()) {
                        val x=size.width/2; val y=size.height/2
                        drawLine(color,Offset(x-14.dp.toPx(),y),Offset(x+14.dp.toPx(),y),3.dp.toPx())
                        drawLine(color,Offset(x,y-14.dp.toPx()),Offset(x,y+14.dp.toPx()),3.dp.toPx())
                    }
                }
                Column(Modifier.heightIn(max=230.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    point?.let { Text(stringResource(Res.string.map_coordinates, formatPlainDecimal(it.latitude), formatPlainDecimal(it.longitude))) }
                    InfoHelp(title = stringResource(Res.string.info_location), text = stringResource(Res.string.map_address_help))
                    TextButton(onClick=userAction {
                        val selected=point
                        if(selected!=null && !busy) {
                            val token=++revision
                            job?.cancel(); busy=true; lookupError=null; zone=null; useZone=false
                            job=scope.launch {
                                try {
                                    when(val result=metadata.suggestTimeZone(selected)) {
                                        is Result.Success -> if(currentPoint==selected && revision==token) { zone=result.data; zonePoint=selected }
                                        is Result.Error -> if(currentPoint==selected && revision==token) lookupError=result.error
                                    }
                                } catch(e: CancellationException) { throw e }
                                finally { if(currentPoint==selected && revision==token) busy=false }
                            }
                        }
                    },enabled=point!=null && !busy) { Text(if(busy) stringResource(Res.string.map_zone_loading) else stringResource(Res.string.map_suggest_zone)) }
                    InfoHelp(title = stringResource(Res.string.info_zone), text = stringResource(Res.string.map_zone_privacy))
                    lookupError?.let { Text(localizedAppErrorMessage(it)) }
                    zone?.let { suggested ->
                        Text(stringResource(Res.string.map_zone_suggestion, suggested))
                        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked=useZone,onCheckedChange={ activity(); useZone=it })
                            Text(stringResource(Res.string.map_use_zone, suggested))
                        }
                    }
                    TextButton(onClick=userAction { uri.openUri("https://openfreemap.org/") }) { Text(stringResource(Res.string.map_attribution)) }
                    TextButton(onClick=userAction { uri.openUri("https://www.openstreetmap.org/copyright") }) { Text(stringResource(Res.string.map_licence)) }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick=userAction(onDismiss),modifier=Modifier.weight(1f)) { Text(stringResource(Res.string.cancel_action)) }
                    Button(onClick=userAction { point?.let { onUse(it,if(useZone && zonePoint==point) zone else null) } },enabled=point!=null && !busy,modifier=Modifier.weight(1f)) { Text(stringResource(Res.string.map_use_point)) }
                }
            }
        }
    }
}
