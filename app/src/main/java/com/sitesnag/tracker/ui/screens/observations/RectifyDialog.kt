package com.sitesnag.tracker.ui.screens.observations

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.sitesnag.tracker.data.local.entity.ObservationEntity
import com.sitesnag.tracker.ui.theme.NavyPrimary
import com.sitesnag.tracker.ui.theme.RectifiedGreen
import com.sitesnag.tracker.ui.theme.SurfaceBorder
import com.sitesnag.tracker.util.CameraHelper
import com.sitesnag.tracker.util.ImageCompressor
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RectifyDialog(
    observation: ObservationEntity,
    onDismiss: () -> Unit,
    onConfirm: (rectifiedDate: Long, afterPhotoPath: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var afterPhotoPath by remember { mutableStateOf<String?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    // Direct Camera Capture with FileProvider
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            coroutineScope.launch {
                isProcessingPhoto = true
                try {
                    val path = ImageCompressor.compressAndSaveImage(
                        context,
                        pendingCameraUri!!,
                        "closure_${observation.entryNumber}"
                    )
                    afterPhotoPath = path
                } catch (e: Exception) {
                    // Ignore photo error
                } finally {
                    isProcessingPhoto = false
                }
            }
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
                // Ignore uri error
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
                // Camera helper error
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery Picker
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isProcessingPhoto = true
                try {
                    val path = ImageCompressor.compressAndSaveImage(
                        context,
                        it,
                        "closure_${observation.entryNumber}"
                    )
                    afterPhotoPath = path
                } catch (e: Exception) {
                    // Ignore photo error
                } finally {
                    isProcessingPhoto = false
                }
            }
        }
    }

    val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RectifiedGreen)
                Spacer(Modifier.width(8.dp))
                Text("Mark as Rectified", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${observation.entryNumber}: ${observation.issueDescription}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Rectification Date: $todayStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = RectifiedGreen,
                    fontWeight = FontWeight.SemiBold
                )

                // Optional After Photo Header
                Text("After / Rectification Photo (Optional):", style = MaterialTheme.typography.labelSmall)

                if (afterPhotoPath != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = File(afterPhotoPath!!),
                            contentDescription = "After Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Explicit Side-by-Side Photo Actions: Take Photo (Primary) + From Gallery (Secondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { launchCamera() },
                        enabled = !isProcessingPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        if (isProcessingPhoto) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (afterPhotoPath == null) "Take Photo" else "Retake", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { galleryPickerLauncher.launch("image/*") },
                        enabled = !isProcessingPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("From Gallery", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(System.currentTimeMillis(), afterPhotoPath) },
                colors = ButtonDefaults.buttonColors(containerColor = RectifiedGreen)
            ) {
                Text("Confirm Rectification")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
