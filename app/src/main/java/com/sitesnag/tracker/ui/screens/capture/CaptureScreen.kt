package com.sitesnag.tracker.ui.screens.capture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sitesnag.tracker.domain.model.CustomFieldType
import com.sitesnag.tracker.domain.model.Priority
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.ui.theme.*
import com.sitesnag.tracker.util.CameraHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Handle Messages
    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Camera Capture & Gallery Pickers
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            viewModel.onPhotoCaptured(pendingCameraUri!!)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = CameraHelper.createTempCameraUri(context)
                pendingCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                // Handle uri creation failure
            }
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val uri = CameraHelper.createTempCameraUri(context)
                pendingCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                // Camera helper fallback
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onPhotoCaptured(it) }
    }

    // Speech-to-Text Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val current = uiState.issueDescription
                viewModel.onIssueDescriptionChanged(if (current.isBlank()) spokenText else "$current $spokenText")
            }
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
        // --- ACTIVE PROJECT BANNER & LOGGER BADGE ---
        Surface(
            color = NavyPrimary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    Icon(Icons.Default.Apartment, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${uiState.activeProject.projectName} (${uiState.activeProject.projectCode})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = NavyPrimary
                    )
                }
                val profile = remember { com.sitesnag.tracker.data.local.EngineerProfileManager.getInstance(context).getProfile() }
                Surface(
                    color = NavyPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Logger: ${profile.initials}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // --- 1. SEGMENTED TOGGLE (OBSERVATION VS NCR) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Normal Observation Tab
                Button(
                    onClick = { viewModel.onRecordTypeChanged(RecordType.NORMAL_OBSERVATION) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.recordType == RecordType.NORMAL_OBSERVATION) NavyPrimary else Color(0xFFEDF2F7),
                        contentColor = if (uiState.recordType == RecordType.NORMAL_OBSERVATION) Color.White else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Normal Snag", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // NCR Tab
                Button(
                    onClick = { viewModel.onRecordTypeChanged(RecordType.NCR) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.recordType == RecordType.NCR) NcrRed else Color(0xFFEDF2F7),
                        contentColor = if (uiState.recordType == RecordType.NCR) Color.White else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("NCR Issue", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // --- 2. PHOTO CAPTURE CARD (Direct Camera Primary + Gallery Secondary) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (uiState.recordType == RecordType.NCR) "Defect Evidence Photo *" else "Site Photo",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.recordType == RecordType.NCR) NcrRed else TextPrimary
                )
                Spacer(Modifier.height(8.dp))

                if (uiState.photoPath != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = File(uiState.photoPath!!),
                            contentDescription = "Site Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { viewModel.onRemovePhoto() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Explicit Side-by-Side Photo Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Action: Take Photo (Direct Camera)
                    Button(
                        onClick = { launchCamera() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.recordType == RecordType.NCR) NcrRed else NavyPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (uiState.photoPath == null) "Take Photo" else "Retake Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Secondary Action: From Gallery
                    OutlinedButton(
                        onClick = { galleryPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("From Gallery", fontSize = 13.sp)
                    }
                }
            }
        }

        // --- 3. CASCADING LOCATION HIERARCHY ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Location Hierarchy", style = MaterialTheme.typography.titleMedium)

                // Block Dropdown
                var blockExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = blockExpanded,
                    onExpandedChange = { blockExpanded = !blockExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedBlock?.name ?: "Select Block / Zone",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Block / Zone") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = blockExpanded,
                        onDismissRequest = { blockExpanded = false }
                    ) {
                        uiState.blocks.forEach { block ->
                            DropdownMenuItem(
                                text = { Text(block.name) },
                                onClick = {
                                    viewModel.onBlockSelected(block)
                                    blockExpanded = false
                                }
                            )
                        }
                    }
                }

                // Floor Dropdown
                var floorExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = floorExpanded,
                    onExpandedChange = { floorExpanded = !floorExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedFloor?.name ?: "Select Floor / Level",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Floor / Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = floorExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = floorExpanded,
                        onDismissRequest = { floorExpanded = false }
                    ) {
                        uiState.floors.forEach { floor ->
                            DropdownMenuItem(
                                text = { Text(floor.name) },
                                onClick = {
                                    viewModel.onFloorSelected(floor)
                                    floorExpanded = false
                                }
                            )
                        }
                    }
                }

                // Preset Sub-location chips with FlowRow wrapping
                if (uiState.subLocations.isNotEmpty()) {
                    Text("Preset Sub-locations:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.subLocations.forEach { sub ->
                            FilterChip(
                                selected = uiState.specificLocationText == sub.name,
                                onClick = { viewModel.onSubLocationPresetSelected(sub) },
                                label = { Text(sub.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Specific Location Free Text
                OutlinedTextField(
                    value = uiState.specificLocationText,
                    onValueChange = { viewModel.onSpecificLocationTextChanged(it) },
                    label = { Text("Specific Location / Room / Grid") },
                    placeholder = { Text("e.g. Flat 302, Master Bath, Grid C-4") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- 4. VENDOR & TRADE SELECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Subcontractor & Trade", style = MaterialTheme.typography.titleMedium)

                var vendorExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = vendorExpanded,
                    onExpandedChange = { vendorExpanded = !vendorExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedVendor?.name ?: "Select Vendor",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vendor Name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vendorExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = vendorExpanded,
                        onDismissRequest = { vendorExpanded = false }
                    ) {
                        uiState.vendors.forEach { vendor ->
                            DropdownMenuItem(
                                text = { Text("${vendor.name} (${vendor.trade})") },
                                onClick = {
                                    viewModel.onVendorSelected(vendor)
                                    vendorExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.tradeText,
                    onValueChange = { viewModel.onTradeChanged(it) },
                    label = { Text("Trade / Work Category") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- 5. PRIORITY & TARGET RESOLUTION DATE ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Priority & Target Date", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.values().forEach { prio ->
                        val isSelected = uiState.priority == prio
                        val color = when (prio) {
                            Priority.HIGH -> NcrRed
                            Priority.MEDIUM -> WarningOrange
                            Priority.LOW -> RectifiedGreen
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onPrioritySelected(prio) },
                            label = { Text(prio.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor = color
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Quick Target Date Chips (+3, +7, +14 Days)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val daysOptions = listOf(3, 7, 14)
                    daysOptions.forEach { days ->
                        val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
                        OutlinedButton(
                            onClick = { viewModel.onTargetDateSelected(targetCal.timeInMillis) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+$days Days", fontSize = 11.sp)
                        }
                    }
                }

                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(uiState.targetDateMillis))
                Text("Target Resolution: $dateStr", style = MaterialTheme.typography.bodyMedium, color = NavyPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        // --- 6. ISSUE DESCRIPTION WITH VOICE-TO-TEXT ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Issue Description *", style = MaterialTheme.typography.titleMedium)
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the defect or observation...")
                            }
                            speechLauncher.launch(intent)
                        }
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice to text", tint = NavySecondary)
                    }
                }

                OutlinedTextField(
                    value = uiState.issueDescription,
                    onValueChange = { viewModel.onIssueDescriptionChanged(it) },
                    placeholder = { Text("Detailed non-conformance or snag observation notes...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- 7. CONDITIONAL NCR FIELDS ---
        if (uiState.recordType == RecordType.NCR) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NcrRedLight.copy(alpha = 0.3f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NcrRed)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🚨 NCR Formal Investigation Details", style = MaterialTheme.typography.titleMedium, color = NcrRed)

                    OutlinedTextField(
                        value = uiState.rootCause,
                        onValueChange = { viewModel.onRootCauseChanged(it) },
                        label = { Text("Root Cause Analysis") },
                        placeholder = { Text("e.g. Lack of surface preparation, incorrect membrane overlap") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.correctiveAction,
                        onValueChange = { viewModel.onCorrectiveActionChanged(it) },
                        label = { Text("Corrective Action Required") },
                        placeholder = { Text("e.g. Remove defective coating, re-prime, re-apply per spec") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- 8. DYNAMIC CUSTOM FIELDS ---
        if (uiState.customFields.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Project Custom Fields", style = MaterialTheme.typography.titleMedium)

                    uiState.customFields.forEach { field ->
                        val value = uiState.customFieldValues[field.id] ?: ""

                        if (field.type == CustomFieldType.DROPDOWN) {
                            val options: List<String> = try {
                                val type = object : TypeToken<List<String>>() {}.type
                                Gson().fromJson(field.optionsJson, type) ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = if (value.isBlank()) "Select ${field.name}" else value,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(field.name + if (field.isRequired) " *" else "") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    options.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                viewModel.onCustomFieldValueChanged(field.id, opt)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { viewModel.onCustomFieldValueChanged(field.id, it) },
                                label = { Text(field.name + if (field.isRequired) " *" else "") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // --- 9. SUBMIT BUTTON ---
        Button(
            onClick = { viewModel.submitRecord() },
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.recordType == RecordType.NCR) NcrRed else NavyPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    imageVector = if (uiState.recordType == RecordType.NCR) Icons.Default.PictureAsPdf else Icons.Default.Save,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (uiState.recordType == RecordType.NCR) "🚨 Issue NCR & Generate A4 PDF" else "Log Site Observation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}
