package com.sitesnag.tracker.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitesnag.tracker.data.local.EngineerProfileManager
import com.sitesnag.tracker.ui.theme.*

@Composable
fun ProjectSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProjects: () -> Unit,
    onNavigateToLocations: () -> Unit,
    onNavigateToVendors: () -> Unit,
    onNavigateToCustomFields: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val profileManager = remember { EngineerProfileManager.getInstance(context) }
    val engineerProfile by profileManager.profileFlow.collectAsState()

    var engName by remember(engineerProfile.fullName) { mutableStateOf(engineerProfile.fullName) }
    var engInitials by remember(engineerProfile.initials) { mutableStateOf(engineerProfile.initials) }

    var projectName by remember(uiState.project.projectName) { mutableStateOf(uiState.project.projectName) }
    var projectCode by remember(uiState.project.projectCode) { mutableStateOf(uiState.project.projectCode) }
    var clientContractor by remember(uiState.project.clientContractorName) { mutableStateOf(uiState.project.clientContractorName) }
    var spreadsheetId by remember(uiState.project.spreadsheetId) { mutableStateOf(uiState.project.spreadsheetId) }
    var defaultCc by remember(uiState.project.defaultCcEmails) { mutableStateOf(uiState.project.defaultCcEmails) }
    var frequency by remember(uiState.project.emailFrequency) { mutableStateOf(uiState.project.emailFrequency) }

    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let {
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
        // --- 0. ENGINEER PROFILE CARD (MULTI-USER COLLABORATION) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Engineer Device Profile (Multi-User IDs)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Your initials prefix your offline logs (e.g. OBS-${engInitials.ifBlank { "SE" }}-001) to eliminate ID collisions across site engineers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = engName,
                    onValueChange = { engName = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Rahul Kumar") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = engInitials,
                    onValueChange = { engInitials = it.uppercase().take(4) },
                    label = { Text("Engineer Initials (2-4 Letters) *") },
                    placeholder = { Text("e.g. RK, AS, JDM") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        profileManager.saveProfile(engName, engInitials)
                        Toast.makeText(context, "Engineer profile updated: ${profileManager.getProfile().displayTag}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Engineer Profile")
                }
            }
        }

        // --- 1. PROJECT METADATA CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Project Metadata & PDF Header", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = projectCode,
                    onValueChange = { projectCode = it },
                    label = { Text("Project Code *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = clientContractor,
                    onValueChange = { clientContractor = it },
                    label = { Text("Client / General Contractor Name *") },
                    placeholder = { Text("Appears on official NCR PDF header") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = spreadsheetId,
                    onValueChange = { spreadsheetId = it },
                    label = { Text("Google Spreadsheet ID (for Sync)") },
                    placeholder = { Text("Google Sheet ID for this project") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = defaultCc,
                    onValueChange = { defaultCc = it },
                    label = { Text("Default CC Email(s)") },
                    placeholder = { Text("e.g. corporate.outlook@domain.com, lead@qa.com") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sync Email Frequency:", style = MaterialTheme.typography.labelSmall)
                    listOf("Daily", "Alternate Days", "Weekly").forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq, fontSize = 11.sp) }
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateProjectMetadata(projectName, projectCode, clientContractor, spreadsheetId, defaultCc, frequency)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Project Settings")
                }
            }
        }

        // --- 2. MASTER DATA MODULES SUB-NAVIGATION ---
        Text("Master Data & Dynamic Engines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        MasterDataNavigationCard(
            icon = Icons.Default.FolderSpecial,
            title = "Multi-Project Management",
            subtitle = "${uiState.allProjects.size} Projects • Active: ${uiState.project.projectName}",
            onClick = onNavigateToProjects
        )

        MasterDataNavigationCard(
            icon = Icons.Default.Apartment,
            title = "Dynamic Location Hierarchy",
            subtitle = "${uiState.blocks.size} Blocks/Zones • Multilevel Floors & Sub-locations",
            onClick = onNavigateToLocations
        )

        MasterDataNavigationCard(
            icon = Icons.Default.People,
            title = "Vendor & Trade Directory",
            subtitle = "${uiState.vendors.size} Vendors Registered with Emails & CCs",
            onClick = onNavigateToVendors
        )

        MasterDataNavigationCard(
            icon = Icons.Default.Tune,
            title = "Dynamic Custom Fields Engine",
            subtitle = "${uiState.customFields.size} Custom Project Attributes (Text, Number, Dropdowns)",
            onClick = onNavigateToCustomFields
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun MasterDataNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NavyPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = NavyPrimary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextMuted)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
