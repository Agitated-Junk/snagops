package com.sitesnag.tracker.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitesnag.tracker.ui.theme.*
import com.sitesnag.tracker.util.FileSharingHelper
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. PROJECT HEADER CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavyPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = uiState.project.projectName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Project Code: ${uiState.project.projectCode} • ${uiState.project.clientContractorName}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                if (uiState.project.defaultCcEmails.isNotBlank()) {
                    Text(
                        text = "Auto-CC: ${uiState.project.defaultCcEmails}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Sync status indicator pill
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.pendingSyncCount > 0) Icons.Default.CloudQueue else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (uiState.pendingSyncCount > 0) "${uiState.pendingSyncCount} Items Queued for Sync" else "All Records Synced Offline",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // --- 2. EXECUTIVE KPI CARDS GRID ---
        Text("Executive Quality Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                title = "Total Snags",
                value = uiState.kpis.totalObservations.toString(),
                subtitle = "${uiState.kpis.openObservations} Open",
                color = NavyPrimary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Rectified %",
                value = String.format(Locale.getDefault(), "%.1f%%", uiState.kpis.observationClosureRate),
                subtitle = "${uiState.kpis.rectifiedObservations} Resolved",
                color = RectifiedGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                title = "Total NCRs",
                value = uiState.kpis.totalNcrs.toString(),
                subtitle = "${uiState.kpis.openNcrs} Active",
                color = NcrRed,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Overdue Items",
                value = uiState.kpis.overdueItems.toString(),
                subtitle = "Past Target Date",
                color = if (uiState.kpis.overdueItems > 0) NcrRed else TextMuted,
                modifier = Modifier.weight(1f)
            )
        }

        // --- 3. EXPORT & SYNC ACTIONS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Export & Cloud Synchronization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Export .XLSX Button
                Button(
                    onClick = {
                        viewModel.exportExcelWorkbook { filePath ->
                            FileSharingHelper.shareFile(
                                context = context,
                                filePath = filePath,
                                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                chooserTitle = "Share Standalone .XLSX Workbook",
                                subject = "${uiState.project.projectName} - Quality & NCR Register"
                            )
                        }
                    },
                    enabled = !uiState.isExportingExcel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)), // Excel Green
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isExportingExcel) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Compiling Excel Workbook...")
                    } else {
                        Icon(Icons.Default.TableChart, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Standalone Multi-Tab .xlsx", fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "• Generates Executive Dashboard (Live Formulas: COUNTIFS, COUNTA)\n• Embeds Vendor & Block breakdown tables + native charts\n• Embeds compressed Before/After photos directly into table cells",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )

                HorizontalDivider(color = SurfaceBorder)

                // Sync Now Button
                OutlinedButton(
                    onClick = { viewModel.triggerManualSync() },
                    enabled = !uiState.isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = NavyPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Sync Now (Drive & Sheets)", color = NavyPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}
