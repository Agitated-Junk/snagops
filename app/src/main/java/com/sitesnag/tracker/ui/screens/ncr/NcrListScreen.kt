package com.sitesnag.tracker.ui.screens.ncr

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sitesnag.tracker.data.local.entity.ObservationEntity
import com.sitesnag.tracker.domain.model.Priority
import com.sitesnag.tracker.domain.model.Status
import com.sitesnag.tracker.ui.screens.observations.RectifyDialog
import com.sitesnag.tracker.ui.theme.*
import com.sitesnag.tracker.util.FileSharingHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcrListScreen(
    viewModel: NcrListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Show Rectify Dialog if an NCR is selected
    uiState.selectedNcrForRectify?.let { ncr ->
        RectifyDialog(
            observation = ncr,
            onDismiss = { viewModel.onDismissRectifyDialog() },
            onConfirm = { date, photo ->
                viewModel.confirmRectification(ncr.id, date, photo)
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
            placeholder = { Text("Search NCRs by ID, block, vendor, cause...") },
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
                label = { Text("All NCRs (${uiState.ncrs.size})") }
            )
            FilterChip(
                selected = uiState.statusFilter == Status.OPEN,
                onClick = { viewModel.onStatusFilterChanged(Status.OPEN) },
                label = { Text("Open (${uiState.ncrs.count { it.status == Status.OPEN }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NcrRedLight,
                    selectedLabelColor = NcrRed
                )
            )
            FilterChip(
                selected = uiState.statusFilter == Status.RECTIFIED,
                onClick = { viewModel.onStatusFilterChanged(Status.RECTIFIED) },
                label = { Text("Rectified (${uiState.ncrs.count { it.status == Status.RECTIFIED }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RectifiedGreenLight,
                    selectedLabelColor = RectifiedGreen
                )
            )
        }

        // --- 3. NCR CARDS LIST ---
        if (uiState.filteredNcrs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No Non-Conformance Reports (NCRs) logged.", color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredNcrs, key = { it.id }) { item ->
                    NcrCard(
                        ncr = item,
                        onViewPdfClick = {
                            item.pdfPath?.let { path ->
                                FileSharingHelper.openOrShareFile(
                                    context = context,
                                    filePath = path,
                                    mimeType = "application/pdf",
                                    chooserTitle = "Open NCR Document (${item.entryNumber})"
                                )
                            }
                        },
                        onRectifyClick = { viewModel.onOpenRectifyDialog(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun NcrCard(
    ncr: ObservationEntity,
    onViewPdfClick: () -> Unit,
    onRectifyClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val isOverdue = ncr.status == Status.OPEN && ncr.targetDate < System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NcrRed.copy(alpha = 0.4f))),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: NCR ID, Priority, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = NcrRed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = ncr.entryNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = NcrRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = ncr.priority.displayName,
                            color = NcrRed,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = if (ncr.status == Status.RECTIFIED) RectifiedGreenLight else NcrRedLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = ncr.status.displayName,
                        color = if (ncr.status == Status.RECTIFIED) RectifiedGreen else NcrRed,
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
                    text = "${ncr.block} > ${ncr.floor} (${ncr.specificLocation})",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Vendor & Trade
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${ncr.vendorName} • ${ncr.trade}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            // Issue Description
            Text(
                text = ncr.issueDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            // Root Cause & Corrective Action
            if (!ncr.rootCause.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFF7FAFC),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Root Cause:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                        Text(ncr.rootCause, fontSize = 11.sp, color = TextSecondary)
                        if (!ncr.correctiveAction.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Corrective Action:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NavyPrimary)
                            Text(ncr.correctiveAction, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Target Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target Date: ${dateFormat.format(Date(ncr.targetDate))}" + if (isOverdue) " ⚠️ OVERDUE" else "",
                    fontSize = 11.sp,
                    color = if (isOverdue) NcrRed else TextMuted,
                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                )

                if (ncr.rectifiedDate != null) {
                    Text(
                        text = "Rectified: ${dateFormat.format(Date(ncr.rectifiedDate))}",
                        fontSize = 11.sp,
                        color = RectifiedGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Photos Row
            if (ncr.beforePhotoPath != null || ncr.afterPhotoPath != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ncr.beforePhotoPath != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Before", fontSize = 10.sp, color = TextMuted)
                            AsyncImage(
                                model = File(ncr.beforePhotoPath),
                                contentDescription = "Before Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                            )
                        }
                    }
                    if (ncr.afterPhotoPath != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Closure Photo", fontSize = 10.sp, color = RectifiedGreen)
                            AsyncImage(
                                model = File(ncr.afterPhotoPath),
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

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View PDF Button
                OutlinedButton(
                    onClick = onViewPdfClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NcrRed),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("View NCR PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Mark Rectified Button (if open)
                if (ncr.status == Status.OPEN) {
                    Button(
                        onClick = onRectifyClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RectifiedGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rectify", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
