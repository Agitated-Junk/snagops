package com.sitesnag.tracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitesnag.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocationManagementScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showAddBlockDialog by remember { mutableStateOf(false) }
    var newBlockName by remember { mutableStateOf("") }

    var showAddFloorDialog by remember { mutableStateOf(false) }
    var newFloorName by remember { mutableStateOf("") }

    var showAddSubLocDialog by remember { mutableStateOf(false) }
    var newSubLocName by remember { mutableStateOf("") }

    // Dialog: Add Block
    if (showAddBlockDialog) {
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Add Block / Zone") },
            text = {
                OutlinedTextField(
                    value = newBlockName,
                    onValueChange = { newBlockName = it },
                    label = { Text("Block Name (e.g. Tower C, Podium)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addBlock(newBlockName)
                        newBlockName = ""
                        showAddBlockDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Add Floor
    if (showAddFloorDialog) {
        AlertDialog(
            onDismissRequest = { showAddFloorDialog = false },
            title = { Text("Add Floor under ${uiState.selectedBlock?.name}") },
            text = {
                OutlinedTextField(
                    value = newFloorName,
                    onValueChange = { newFloorName = it },
                    label = { Text("Floor / Level Name (e.g. 3rd Floor, Terrace)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addFloor(newFloorName)
                        newFloorName = ""
                        showAddFloorDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFloorDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Add SubLocation
    if (showAddSubLocDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubLocDialog = false },
            title = { Text("Add Sub-location under ${uiState.selectedFloor?.name}") },
            text = {
                OutlinedTextField(
                    value = newSubLocName,
                    onValueChange = { newSubLocName = it },
                    label = { Text("Preset Sub-location (e.g. Flat 301, Shaft 2)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addSubLocation(newSubLocName)
                        newSubLocName = ""
                        showAddSubLocDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubLocDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dynamic Location Hierarchy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceLight,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Context Pill
            Surface(
                color = NavyPrimary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Apartment, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Project: ${uiState.project.projectName} (${uiState.project.projectCode})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = NavyPrimary
                    )
                }
            }

            // --- SECTION 1: BLOCKS / ZONES ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1. Blocks / Zones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAddBlockDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Block")
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.blocks.forEach { block ->
                    val isSelected = uiState.selectedBlock?.id == block.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectBlock(block) },
                        label = { Text(block.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        trailingIcon = {
                            if (isSelected && uiState.blocks.size > 1) {
                                IconButton(
                                    onClick = { viewModel.deleteBlock(block) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete Block", modifier = Modifier.size(12.dp))
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            HorizontalDivider(color = SurfaceBorder)

            // --- SECTION 2: FLOORS / LEVELS ---
            if (uiState.selectedBlock != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("2. Floors in ${uiState.selectedBlock?.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddFloorDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Floor")
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.floors.forEach { floor ->
                        val isSelected = uiState.selectedFloor?.id == floor.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectFloor(floor) },
                            label = { Text(floor.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            trailingIcon = {
                                if (isSelected) {
                                    IconButton(
                                        onClick = { viewModel.deleteFloor(floor) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete Floor", modifier = Modifier.size(12.dp))
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavySecondary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                HorizontalDivider(color = SurfaceBorder)
            }

            // --- SECTION 3: PRESET SUBLOCATIONS ---
            if (uiState.selectedFloor != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3. Presets under ${uiState.selectedFloor?.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddSubLocDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Preset")
                    }
                }

                if (uiState.subLocations.isEmpty()) {
                    Text("No preset sub-locations defined for this floor.", fontSize = 12.sp, color = TextMuted)
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.subLocations.forEach { sub ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(sub.name, fontSize = 13.sp) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.deleteSubLocation(sub) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
