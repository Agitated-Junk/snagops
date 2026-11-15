package com.sitesnag.tracker.ui.screens.observations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sitesnag.tracker.data.local.entity.ObservationEntity
import com.sitesnag.tracker.domain.model.Priority
import com.sitesnag.tracker.domain.model.Status
import com.sitesnag.tracker.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationListScreen(
    viewModel: ObservationListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show Rectify Dialog if an observation is selected
    uiState.selectedObservationForRectify?.let { observation ->
        RectifyDialog(
            observation = observation,
            onDismiss = { viewModel.onDismissRectifyDialog() },
            onConfirm = { date, photo ->
                viewModel.confirmRectification(observation.id, date, photo)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. SEARCH BAR ---
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Search by ID, block, vendor, issue...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        // --- 2. FILTER CHIPS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.statusFilter == null,
                onClick = { viewModel.onStatusFilterChanged(null) },
                label = { Text("All (${uiState.observations.size})") }
            )
            FilterChip(
                selected = uiState.statusFilter == Status.OPEN,
                onClick = { viewModel.onStatusFilterChanged(Status.OPEN) },
                label = { Text("Open (${uiState.observations.count { it.status == Status.OPEN }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WarningOrangeLight,
                    selectedLabelColor = WarningOrange
                )
            )
            FilterChip(
                selected = uiState.statusFilter == Status.RECTIFIED,
                onClick = { viewModel.onStatusFilterChanged(Status.RECTIFIED) },
                label = { Text("Rectified (${uiState.observations.count { it.status == Status.RECTIFIED }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RectifiedGreenLight,
                    selectedLabelColor = RectifiedGreen
                )
            )
        }

        // --- 3. OBSERVATION CARDS LIST ---
        if (uiState.filteredObservations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No site observations found.", color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredObservations, key = { it.id }) { item ->
                    ObservationCard(
                        observation = item,
                        onRectifyClick = { viewModel.onOpenRectifyDialog(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun ObservationCard(
    observation: ObservationEntity,
    onRectifyClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val isOverdue = observation.status == Status.OPEN && observation.targetDate < System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: ID, Priority, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = observation.entryNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NavyPrimary
                    )

                    // Priority Badge
                    val prioColor = when (observation.priority) {
                        Priority.HIGH -> NcrRed
                        Priority.MEDIUM -> WarningOrange
                        Priority.LOW -> RectifiedGreen
                    }
                    Surface(
                        color = prioColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = observation.priority.displayName,
                            color = prioColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = if (observation.status == Status.RECTIFIED) RectifiedGreenLight else WarningOrangeLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = observation.status.displayName,
                        color = if (observation.status == Status.RECTIFIED) RectifiedGreen else WarningOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${observation.block} > ${observation.floor} (${observation.specificLocation})",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Vendor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${observation.vendorName} • ${observation.trade}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            // Description
            Text(
                text = observation.issueDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            // Target Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target: ${dateFormat.format(Date(observation.targetDate))}" + if (isOverdue) " ⚠️ OVERDUE" else "",
                    fontSize = 11.sp,
                    color = if (isOverdue) NcrRed else TextMuted,
                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                )

                if (observation.rectifiedDate != null) {
                    Text(
                        text = "Rectified: ${dateFormat.format(Date(observation.rectifiedDate))}",
                        fontSize = 11.sp,
                        color = RectifiedGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Photos Row
            if (observation.beforePhotoPath != null || observation.afterPhotoPath != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (observation.beforePhotoPath != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Before", fontSize = 10.sp, color = TextMuted)
                            AsyncImage(
                                model = File(observation.beforePhotoPath),
                                contentDescription = "Before Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                            )
                        }
                    }
                    if (observation.afterPhotoPath != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("After / Rectified", fontSize = 10.sp, color = RectifiedGreen)
                            AsyncImage(
                                model = File(observation.afterPhotoPath),
                                contentDescription = "After Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, RectifiedGreenLight, RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }
            }

            // Mark Rectified Button (if open)
            if (observation.status == Status.OPEN) {
                Button(
                    onClick = onRectifyClick,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RectifiedGreen),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mark Rectified", fontSize = 12.sp)
                }
            }
        }
    }
}
