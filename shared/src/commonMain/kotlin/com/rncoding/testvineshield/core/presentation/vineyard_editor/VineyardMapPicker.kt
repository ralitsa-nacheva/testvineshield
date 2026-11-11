package com.rncoding.testvineshield.core.presentation.vineyard_editor

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
    var message by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(point) { revision++; job?.cancel(); zonePoint=null; zone=null; useZone=false; busy=false; message=null }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().userActivity(activity)) {
            Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Choose vineyard location",style=MaterialTheme.typography.titleLarge)
                Text("Online map: OpenFreeMap receives the viewed area and normal network information. Records stay local. No device location permission is used.")
                Text("Pan and zoom until the crosshair is on your vineyard. For a missing/offline map, cancel and use manual coordinates.")
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
                    point?.let { Text("Latitude: ${formatPlainDecimal(it.latitude)} · Longitude: ${formatPlainDecimal(it.longitude)}") }
                    Text("Country and locality remain editable in the form. This basemap does not return a reliable address or nearest village.")
                    TextButton(onClick=userAction {
                        val selected=point
                        if(selected!=null && !busy) {
                            val token=++revision
                            job?.cancel(); busy=true; message=null; zone=null; useZone=false
                            job=scope.launch {
                                try {
                                    when(val result=metadata.suggestTimeZone(selected)) {
                                        is Result.Success -> if(currentPoint==selected && revision==token) { zone=result.data; zonePoint=selected; message="Suggested zone: ${result.data}. Confirm before using." }
                                        is Result.Error -> if(currentPoint==selected && revision==token) message=result.error.userMessage
                                    }
                                } catch(e: CancellationException) { throw e }
                                finally { if(currentPoint==selected && revision==token) busy=false }
                            }
                        }
                    },enabled=point!=null && !busy) { Text(if(busy) "Looking up time zone…" else "Suggest time zone using Open-Meteo") }
                    Text("Time-zone lookup sends this selected point only when requested. It is optional; coordinates can be used without it.")
                    message?.let { Text(it) }
                    zone?.let { suggested ->
                        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked=useZone,onCheckedChange={ activity(); useZone=it })
                            Text("Use $suggested (replaces the form's time zone)")
                        }
                    }
                    TextButton(onClick=userAction { uri.openUri("https://openfreemap.org/") }) { Text("© OpenFreeMap · © OpenMapTiles · Data © OpenStreetMap contributors") }
                    TextButton(onClick=userAction { uri.openUri("https://www.openstreetmap.org/copyright") }) { Text("Map data attribution and licence") }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick=userAction(onDismiss),modifier=Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick=userAction { point?.let { onUse(it,if(useZone && zonePoint==point) zone else null) } },enabled=point!=null && !busy,modifier=Modifier.weight(1f)) { Text("Use point") }
                }
            }
        }
    }
}
