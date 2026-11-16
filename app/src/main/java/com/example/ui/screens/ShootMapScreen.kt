package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.ShootLocation
import com.example.data.model.StudioProject
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.UiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private data class CloudRenderHub(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val spec: String,
)

private val GLOBAL_RENDER_HUBS = listOf(
    CloudRenderHub("hub_tokyo", "SuSa Tokyo GPU-01", 35.6762, 139.6503, "8x H100 • 14ms Asia"),
    CloudRenderHub("hub_oregon", "SuSa Oregon Render-04", 45.5152, -122.6784, "16x L40S • US-West"),
    CloudRenderHub("hub_frankfurt", "SuSa Frankfurt Vault-02", 50.1109, 8.6821, "AES-256 EU Vault"),
)

private const val DARK_OBSIDIAN_MAP_STYLE = """
[
  {"elementType":"geometry","stylers":[{"color":"#111827"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#9ca3af"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#0b0f19"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1f2937"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0b0f19"}]}
]
"""

@Composable
fun ShootMapScreen(
    locationsState: UiState<List<ShootLocation>>,
    projectsState: UiState<List<StudioProject>>,
    selectedProjectId: String?,
    onSaveLocation: (name: String, category: String, lat: Double, lng: Double, notes: String, projectId: String?) -> Unit,
    onAttachLocationToProject: (projectId: String, location: ShootLocation) -> Unit,
    onDeleteLocation: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locations = (locationsState as? UiState.Success)?.data.orEmpty()
    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val activeProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()

    var selectedCategory by remember { mutableStateOf("All") }
    var pendingPinLatLng by remember { mutableStateOf<LatLng?>(null) }
    var showAddPinDialog by remember { mutableStateOf(false) }

    val initialLatLng = remember(locations) {
        locations.firstOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(35.6595, 139.7004)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 11f)
    }

    val mapKeyConfigured = remember {
        BuildConfig.MAPS_API_KEY.isNotBlank() && BuildConfig.MAPS_API_KEY != "MY_MAPS_API_KEY"
    }

    val filteredLocations = remember(locations, selectedCategory) {
        if (selectedCategory == "All" || selectedCategory == "Render Hub") {
            locations
        } else {
            locations.filter { it.category == selectedCategory }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shoot Locations & Render Map", style = MaterialTheme.typography.headlineSmall, color = TextMain)
                    Text(
                        text = "Tap map to drop a filming pin or inspect SuSa GPU nodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Button(
                    onClick = {
                        pendingPinLatLng = cameraPositionState.position.target
                        showAddPinDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier.testTag("add_shoot_pin_button"),
                ) {
                    Icon(Icons.Filled.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pin Spot")
                }
            }
        }

        if (!mapKeyConfigured) {
            item {
                Surface(
                    color = AmberWarning.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Google Maps tiles require MAPS_API_KEY in the AI Studio Secrets panel. Pins, coordinates, and project geotagging remain active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        item {
            val categories = listOf("All", "Exterior", "Studio", "Aerial", "Render Hub")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary),
                    )
                }
            }
        }

        // Interactive Google Map Canvas
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(StudioSurface)
                    .testTag("shoot_google_map"),
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapStyleOptions = remember { MapStyleOptions(DARK_OBSIDIAN_MAP_STYLE) }
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        compassEnabled = true,
                        mapToolbarEnabled = false,
                    ),
                    onMapClick = { latLng ->
                        pendingPinLatLng = latLng
                        showAddPinDialog = true
                    },
                ) {
                    if (selectedCategory != "Render Hub") {
                        filteredLocations.forEach { loc ->
                            val markerState = remember(loc.id, loc.latitude, loc.longitude) {
                                MarkerState(position = LatLng(loc.latitude, loc.longitude))
                            }
                            Marker(
                                state = markerState,
                                title = loc.name,
                                snippet = "${loc.category} • ${loc.scoutingNotes}",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                            )
                        }
                    }

                    if (selectedCategory == "All" || selectedCategory == "Render Hub") {
                        GLOBAL_RENDER_HUBS.forEach { hub ->
                            val hubState = remember(hub.id) {
                                MarkerState(position = LatLng(hub.lat, hub.lng))
                            }
                            Marker(
                                state = hubState,
                                title = hub.name,
                                snippet = hub.spec,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            )
                        }
                    }
                }
            }
        }

        // Pinned Shoot Locations List
        item {
            Text(
                text = "Pinned Shoot Locations (${filteredLocations.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextMain,
            )
        }

        if (filteredLocations.isEmpty() && selectedCategory != "Render Hub") {
            item {
                Surface(
                    color = StudioSurface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("No shoot pins yet", style = MaterialTheme.typography.titleSmall, color = TextMain)
                        Text(
                            text = "Tap anywhere on the Google Map above or use AI Location Scout in the AI Lab to pin real filming coordinates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
            }
        } else if (selectedCategory != "Render Hub") {
            items(filteredLocations, key = { it.id }) { loc ->
                ShootLocationCard(
                    location = loc,
                    activeProject = activeProject,
                    onFocusMap = {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 14f)
                            )
                        }
                    },
                    onAttachToProject = {
                        if (activeProject != null) {
                            onAttachLocationToProject(activeProject.id, loc)
                        }
                    },
                    onOpenDirections = {
                        val uri = Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(${Uri.encode(loc.name)})")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                    onDelete = { onDeleteLocation(loc.id) },
                )
            }
        }

        // Global Cloud Render Nodes
        if (selectedCategory == "All" || selectedCategory == "Render Hub") {
            item {
                Text(
                    text = "Global Cloud GPU Render Hubs",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMain,
                )
            }
            items(GLOBAL_RENDER_HUBS, key = { it.id }) { hub ->
                Surface(
                    color = StudioSurface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(hub.lat, hub.lng), 10f)
                                )
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.CloudQueue, contentDescription = null, tint = CyanAccent)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(hub.name, style = MaterialTheme.typography.titleSmall, color = TextMain)
                            Text(hub.spec, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Text(
                            text = "${String.format("%.2f", hub.lat)}, ${String.format("%.2f", hub.lng)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndigoLight,
                        )
                    }
                }
            }
        }
    }

    if (showAddPinDialog) {
        val target = pendingPinLatLng ?: LatLng(35.6595, 139.7004)
        AddShootPinDialog(
            initialLat = target.latitude,
            initialLng = target.longitude,
            activeProjectTitle = activeProject?.title,
            onDismiss = { showAddPinDialog = false },
            onConfirm = { name, category, lat, lng, notes, attach ->
                showAddPinDialog = false
                onSaveLocation(
                    name,
                    category,
                    lat,
                    lng,
                    notes,
                    if (attach) activeProject?.id else null,
                )
            },
        )
    }
}

@Composable
private fun ShootLocationCard(
    location: ShootLocation,
    activeProject: StudioProject?,
    onFocusMap: () -> Unit,
    onAttachToProject: () -> Unit,
    onOpenDirections: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = StudioSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFocusMap),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(18.dp))
                    Text(location.name, style = MaterialTheme.typography.titleMedium, color = TextMain)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete pin", tint = TextMuted)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = IndigoPrimary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = location.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = IndigoLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            if (location.scoutingNotes.isNotBlank()) {
                Text(location.scoutingNotes, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activeProject != null) {
                    OutlinedButton(
                        onClick = onAttachToProject,
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tag \"${activeProject.title.take(16)}\"", color = EmeraldSuccess)
                    }
                }
                OutlinedButton(
                    onClick = onOpenDirections,
                    border = BorderStroke(1.dp, StudioBorder),
                ) {
                    Icon(Icons.Filled.Directions, contentDescription = null, tint = TextMain, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Navigate", color = TextMain)
                }
            }
        }
    }
}

@Composable
private fun AddShootPinDialog(
    initialLat: Double,
    initialLng: Double,
    activeProjectTitle: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, lat: Double, lng: Double, notes: String, attachToProject: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Exterior") }
    var notes by remember { mutableStateOf("") }
    var attachToProject by remember { mutableStateOf(activeProjectTitle != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = { Text("Pin Shoot Location", color = TextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Coordinates: ${String.format("%.5f", initialLat)}, ${String.format("%.5f", initialLng)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name") },
                    placeholder = { Text("e.g. Rooftop Helipad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Exterior", "Studio", "Aerial").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary),
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Lighting, lens, or permit notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (activeProjectTitle != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { attachToProject = !attachToProject },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Attach to \"$activeProjectTitle\"", style = MaterialTheme.typography.bodySmall, color = TextMain)
                        androidx.compose.material3.Checkbox(
                            checked = attachToProject,
                            onCheckedChange = { attachToProject = it },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category, initialLat, initialLng, notes, attachToProject) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            ) {
                Text("Save Pin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
