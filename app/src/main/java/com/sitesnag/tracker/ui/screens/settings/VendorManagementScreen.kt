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
import com.sitesnag.tracker.data.local.entity.VendorEntity
import com.sitesnag.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorManagementScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingVendor by remember { mutableStateOf<VendorEntity?>(null) }

    var name by remember { mutableStateOf("") }
    var trade by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var cc by remember { mutableStateOf("") }

    fun openDialog(vendor: VendorEntity? = null) {
        editingVendor = vendor
        name = vendor?.name ?: ""
        trade = vendor?.trade ?: ""
        email = vendor?.primaryEmail ?: ""
        cc = vendor?.ccEmails ?: ""
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingVendor == null) "Register New Vendor" else "Edit Vendor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Vendor / Subcontractor Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = trade,
                        onValueChange = { trade = it },
                        label = { Text("Trade / Work Category *") },
                        placeholder = { Text("Civil, MEP, Finishing, Waterproofing...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Primary Email Address *") },
                        placeholder = { Text("pm@subcontractor.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cc,
                        onValueChange = { cc = it },
                        label = { Text("CC Email(s)") },
                        placeholder = { Text("director@subcontractor.com, qa@sub.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveVendor(
                            id = editingVendor?.id ?: 0L,
                            name = name,
                            trade = trade,
                            email = email,
                            cc = cc
                        )
                        showDialog = false
                    }
                ) {
                    Text(if (editingVendor == null) "Add Vendor" else "Save")
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
                title = { Text("Vendor & Trade Directory") },
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
                onClick = { openDialog() },
                containerColor = NavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Vendor")
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
            items(uiState.vendors, key = { it.id }) { vendor ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(vendor.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                            Surface(
                                color = NavyPrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = vendor.trade,
                                    color = NavyPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(vendor.primaryEmail, fontSize = 12.sp, color = TextSecondary)
                        }

                        if (vendor.ccEmails.isNotBlank()) {
                            Text("CC: ${vendor.ccEmails}", fontSize = 11.sp, color = TextMuted)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { openDialog(vendor) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavySecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.deleteVendor(vendor) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NcrRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
