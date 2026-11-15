package com.sitesnag.tracker.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.sitesnag.tracker.data.local.entity.ProjectEntity
import com.sitesnag.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsManagementScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showProjectDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }

    var showJoinDialog by remember { mutableStateOf(false) }
    var joinSnippetText by remember { mutableStateOf("") }
    var shareSnippetDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Dialog state
    var projectName by remember { mutableStateOf("") }
    var projectCode by remember { mutableStateOf("") }
    var clientContractor by remember { mutableStateOf("") }
    var spreadsheetId by remember { mutableStateOf("") }
    var defaultCc by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Daily") }

    fun openDialog(project: ProjectEntity? = null) {
        editingProject = project
        if (project != null) {
            projectName = project.projectName
            projectCode = project.projectCode
            clientContractor = project.clientContractorName
            spreadsheetId = project.spreadsheetId
            defaultCc = project.defaultCcEmails
            frequency = project.emailFrequency
        } else {
            projectName = ""
            projectCode = ""
            clientContractor = ""
            spreadsheetId = ""
            defaultCc = ""
            frequency = "Daily"
        }
        showProjectDialog = true
    }

    // Dialog: Join via Config
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Project via Config Code", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste the project JSON config snippet shared by your team lead:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = joinSnippetText,
                        onValueChange = { joinSnippetText = it },
                        placeholder = { Text("{\"name\": \"...\", \"code\": \"...\", \"spreadsheetId\": \"...\"}") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val map = Gson().fromJson(joinSnippetText.trim(), Map::class.java)
                            val name = map["name"]?.toString() ?: ""
                            val code = map["code"]?.toString() ?: ""
                            val client = map["client"]?.toString() ?: ""
                            val sheetId = map["spreadsheetId"]?.toString() ?: ""
                            val cc = map["defaultCc"]?.toString() ?: ""
                            val freq = map["frequency"]?.toString() ?: "Daily"

                            if (name.isNotBlank() && code.isNotBlank()) {
                                viewModel.saveProject(
                                    id = 0L,
                                    name = name,
                                    code = code,
                                    clientContractor = client,
                                    spreadsheetId = sheetId,
                                    defaultCc = cc,
                                    frequency = freq
                                )
                                showJoinDialog = false
                                joinSnippetText = ""
                                Toast.makeText(context, "Joined project '$name' successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid snippet: Project name and code required", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Invalid JSON: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Join Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Share Config Snippet
    shareSnippetDialog?.let { (title, snippet) ->
        AlertDialog(
            onDismissRequest = { shareSnippetDialog = null },
            title = { Text("Share Project: $title", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Copy this JSON config snippet and share it with your teammates so they can join this project immediately in SnagOps:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = snippet,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("SnagOps Project Config", snippet))
                        Toast.makeText(context, "Project config snippet copied to clipboard!", Toast.LENGTH_SHORT).show()
                        shareSnippetDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Config")
                }
            },
            dismissButton = {
                TextButton(onClick = { shareSnippetDialog = null }) { Text("Close") }
            }
        )
    }

    // Dialog: Create or Edit Project
    if (showProjectDialog) {
        AlertDialog(
            onDismissRequest = { showProjectDialog = false },
            title = {
                Text(
                    if (editingProject == null) "Create New Project" else "Edit Project",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name *") },
                        placeholder = { Text("e.g. Harbor Point Tower") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = projectCode,
                        onValueChange = { projectCode = it },
                        label = { Text("Project Code *") },
                        placeholder = { Text("e.g. HPT-2026") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = clientContractor,
                        onValueChange = { clientContractor = it },
                        label = { Text("Client / Contractor Name") },
                        placeholder = { Text("e.g. Apex Builders Ltd") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = spreadsheetId,
                        onValueChange = { spreadsheetId = it },
                        label = { Text("Google Spreadsheet ID (for Sync)") },
                        placeholder = { Text("e.g. 1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = defaultCc,
                        onValueChange = { defaultCc = it },
                        label = { Text("Default CC Email(s)") },
                        placeholder = { Text("qa@domain.com, lead@pmc.com") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Frequency selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Email Frequency:", style = MaterialTheme.typography.labelSmall)
                        listOf("Daily", "Alternate Days", "Weekly").forEach { freq ->
                            FilterChip(
                                selected = frequency == freq,
                                onClick = { frequency = freq },
                                label = { Text(freq, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (projectName.isNotBlank() && projectCode.isNotBlank()) {
                            viewModel.saveProject(
                                id = editingProject?.id ?: 0L,
                                name = projectName,
                                code = projectCode,
                                clientContractor = clientContractor,
                                spreadsheetId = spreadsheetId,
                                defaultCc = defaultCc,
                                frequency = frequency
                            )
                            showProjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text(if (editingProject == null) "Create Project" else "Update Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Construction Projects") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showJoinDialog = true }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Join Project via Config", tint = Color.White)
                    }
                    IconButton(onClick = { openDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Project", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openDialog() },
                containerColor = NavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Construction Projects (${uiState.allProjects.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                OutlinedButton(
                    onClick = { showJoinDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Join via Config", fontSize = 11.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.allProjects, key = { it.id }) { project ->
                    val isActive = project.id == uiState.project.id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) Color(0xFFF0F7FF) else SurfaceLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.projectName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Code: ${project.projectCode} • Client: ${project.clientContractorName.ifBlank { "Not Set" }}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                if (isActive) {
                                    Surface(
                                        color = RectifiedGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = RectifiedGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Active",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = RectifiedGreen
                                            )
                                        }
                                    }
                                }
                            }

                            if (project.spreadsheetId.isNotBlank()) {
                                Text(
                                    text = "Google Sheet: ${project.spreadsheetId}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            HorizontalDivider(color = SurfaceBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isActive) {
                                    Button(
                                        onClick = { viewModel.switchActiveProject(project.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Set as Active", fontSize = 12.sp)
                                    }
                                } else {
                                    Text(
                                        "Working Project",
                                        fontSize = 12.sp,
                                        color = NavyPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Share Config Snippet Button
                                    IconButton(
                                        onClick = {
                                            val snippetMap = mapOf(
                                                "name" to project.projectName,
                                                "code" to project.projectCode,
                                                "client" to project.clientContractorName,
                                                "spreadsheetId" to project.spreadsheetId,
                                                "defaultCc" to project.defaultCcEmails,
                                                "frequency" to project.emailFrequency
                                            )
                                            val json = Gson().toJson(snippetMap)
                                            shareSnippetDialog = Pair(project.projectName, json)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Project Config", tint = NavySecondary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { openDialog(project) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }

                                    if (uiState.allProjects.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteProject(project) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NcrRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
