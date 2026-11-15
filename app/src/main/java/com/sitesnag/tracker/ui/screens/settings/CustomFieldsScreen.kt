package com.sitesnag.tracker.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sitesnag.tracker.domain.model.CustomFieldType
import com.sitesnag.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFieldsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var fieldName by remember { mutableStateOf("") }
    var fieldType by remember { mutableStateOf(CustomFieldType.TEXT) }
    var dropdownOptionsText by remember { mutableStateOf("") }
    var isRequired by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Define Dynamic Custom Field") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fieldName,
                        onValueChange = { fieldName = it },
                        label = { Text("Field Display Name *") },
                        placeholder = { Text("e.g. Drawing Ref, Inspection Stage, Package") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type Selector
                    Text("Field Type:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CustomFieldType.values().forEach { type ->
                            val isSel = fieldType == type
                            FilterChip(
                                selected = isSel,
                                onClick = { fieldType = type },
                                label = { Text(type.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (fieldType == CustomFieldType.DROPDOWN) {
                        OutlinedTextField(
                            value = dropdownOptionsText,
                            onValueChange = { dropdownOptionsText = it },
                            label = { Text("Dropdown Options (comma-separated)") },
                            placeholder = { Text("Option 1, Option 2, Option 3") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRequired,
                            onCheckedChange = { isRequired = it }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Mandatory / Required Field", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val options = if (fieldType == CustomFieldType.DROPDOWN) {
                            dropdownOptionsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        } else emptyList()

                        viewModel.saveCustomField(
                            name = fieldName,
                            type = fieldType,
                            options = options,
                            isRequired = isRequired
                        )
                        fieldName = ""
                        dropdownOptionsText = ""
                        isRequired = false
                        showDialog = false
                    }
                ) {
                    Text("Save Field")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dynamic Custom Fields Engine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight, titleContentColor = TextPrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = NavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Field")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.customFields, key = { it.id }) { field ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(field.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (field.isRequired) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(color = NcrRedLight, shape = RoundedCornerShape(4.dp)) {
                                        Text("Required", color = NcrRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text("Type: ${field.type.displayName}", fontSize = 12.sp, color = NavySecondary)

                            if (field.type == CustomFieldType.DROPDOWN) {
                                val opts: List<String> = try {
                                    val type = object : TypeToken<List<String>>() {}.type
                                    Gson().fromJson(field.optionsJson, type) ?: emptyList()
                                } catch (e: Exception) {
                                    emptyList()
                                }
                                if (opts.isNotEmpty()) {
                                    Text("Options: ${opts.joinToString(", ")}", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.deleteCustomField(field) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NcrRed)
                        }
                    }
                }
            }
        }
    }
}
