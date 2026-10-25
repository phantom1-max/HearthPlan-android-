package com.example.ui.components.dialogs

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.MeasurementUnit
import com.example.util.PlotVisionAnalyzer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlankProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (
        name: String,
        width1Meters: Float,
        width2Meters: Float,
        length1Meters: Float,
        length2Meters: Float,
        unit: MeasurementUnit,
        floors: Int
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var projectName by remember { mutableStateOf("New Residence") }
    var unit by remember { mutableStateOf(MeasurementUnit.METERS) }

    // 4-sided dimensions
    var width1Input by remember { mutableStateOf("15.0") }
    var width2Input by remember { mutableStateOf("15.0") }
    var length1Input by remember { mutableStateOf("20.0") }
    var length2Input by remember { mutableStateOf("20.0") }

    var isIrregularPlot by remember { mutableStateOf(false) }

    // Number of floors
    var floorCountInput by remember { mutableStateOf("1") }

    // Photo / Sketch AI Analysis State
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzingImage by remember { mutableStateOf(false) }
    var aiAnalysisNote by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun processCapturedImage(bitmap: Bitmap?) {
        if (bitmap == null) return
        selectedImageBitmap = bitmap
        isAnalyzingImage = true
        aiAnalysisNote = null
        errorMessage = null
        scope.launch {
            try {
                val result = PlotVisionAnalyzer.analyzePlotImage(bitmap, unit)
                width1Input = String.format(java.util.Locale.US, "%.1f", result.width1)
                width2Input = String.format(java.util.Locale.US, "%.1f", result.width2)
                length1Input = String.format(java.util.Locale.US, "%.1f", result.length1)
                length2Input = String.format(java.util.Locale.US, "%.1f", result.length2)
                if (result.width1 != result.width2 || result.length1 != result.length2) {
                    isIrregularPlot = true
                }
                floorCountInput = result.recommendedFloors.toString()
                aiAnalysisNote = result.notes
            } catch (e: Exception) {
                errorMessage = "Scan analysis error: ${e.message}"
            } finally {
                isAnalyzingImage = false
            }
        }
    }

    // High-resolution File-based Camera Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = tempCameraUri
        if (success && uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                processCapturedImage(bitmap)
            } catch (e: Exception) {
                errorMessage = "Could not decode captured photo: ${e.message}"
            }
        }
    }

    // Camera runtime permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                val tempFile = java.io.File(context.cacheDir, "scan_plot_${System.currentTimeMillis()}.jpg")
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                errorMessage = "Storage error launching camera: ${e.message}"
            }
        } else {
            errorMessage = "Camera permission is required to capture photos of plot plans."
        }
    }

    fun startCamera() {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                val tempFile = java.io.File(context.cacheDir, "scan_plot_${System.currentTimeMillis()}.jpg")
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                errorMessage = "Could not launch camera: ${e.message}"
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Photo Picker launcher (zero-permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                processCapturedImage(bitmap)
            } catch (e: Exception) {
                errorMessage = "Could not load image: ${e.message}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SquareFoot, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Blank Project", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Define your plot dimensions manually or upload a picture of your plot plan / sketch for AI to analyze automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )

                // 1. Choose Preferred Unit (Meters, Feet, Inches, Centimeters, Millimeters, Yards)
                Text("Preferred Measurement Unit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (u in MeasurementUnit.values()) {
                        FilterChip(
                            selected = unit == u,
                            onClick = {
                                if (unit != u) {
                                    // Convert existing inputs to the new unit
                                    val oldW1 = unit.fromDisplayValue(width1Input.toFloatOrNull() ?: 15f)
                                    val oldW2 = unit.fromDisplayValue(width2Input.toFloatOrNull() ?: 15f)
                                    val oldL1 = unit.fromDisplayValue(length1Input.toFloatOrNull() ?: 20f)
                                    val oldL2 = unit.fromDisplayValue(length2Input.toFloatOrNull() ?: 20f)
                                    unit = u
                                    width1Input = String.format(java.util.Locale.US, "%.1f", u.toDisplayValue(oldW1))
                                    width2Input = String.format(java.util.Locale.US, "%.1f", u.toDisplayValue(oldW2))
                                    length1Input = String.format(java.util.Locale.US, "%.1f", u.toDisplayValue(oldL1))
                                    length2Input = String.format(java.util.Locale.US, "%.1f", u.toDisplayValue(oldL2))
                                }
                            },
                            label = { Text("${u.displayName} (${u.symbol})") }
                        )
                    }
                }

                // 2. Picture Upload / Capture & AI Analysis Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Photo & Sketch Analysis", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }

                        Text(
                            "Take a photo or upload a sketch of your plot or blueprint. The AI scanner reads written measurements (handwritten or printed, e.g. 19m, 25 ft) and automatically maps them to your boundary inputs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { startCamera() },
                                modifier = Modifier.weight(1f).testTag("capture_photo_button")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Take Photo", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("pick_gallery_button")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("From Gallery", maxLines = 1)
                            }
                        }

                        // Instant Sample Blueprint for Testing
                        OutlinedButton(
                            onClick = {
                                processCapturedImage(PlotVisionAnalyzer.createSampleSketchBitmap())
                            },
                            modifier = Modifier.fillMaxWidth().testTag("try_sample_sketch_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Architecture, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Sample Sketch (19m × 25m Irregular)", maxLines = 1)
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        errorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        if (selectedImageBitmap != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = selectedImageBitmap!!.asImageBitmap(),
                                    contentDescription = "Uploaded sketch",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (isAnalyzingImage) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("AI analyzing plot boundary...", style = MaterialTheme.typography.bodySmall)
                                        }
                                    } else {
                                        Text(
                                            aiAnalysisNote ?: "Dimensions extracted from image!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                IconButton(onClick = { selectedImageBitmap = null; aiAnalysisNote = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove photo", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // 3. 2 Width & 2 Length Dimension Inputs (Irregular vs Rectangular)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isIrregularPlot) "4-Sided Plot Dimensions (${unit.symbol})" else "Plot Dimensions (${unit.symbol})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FilterChip(
                        selected = isIrregularPlot,
                        onClick = {
                            isIrregularPlot = !isIrregularPlot
                            if (!isIrregularPlot) {
                                width2Input = width1Input
                                length2Input = length1Input
                            }
                        },
                        label = { Text(if (isIrregularPlot) "Different Sides" else "Rectangle") },
                        modifier = Modifier.testTag("toggle_irregular_plot")
                    )
                }

                if (!isIrregularPlot) {
                    // Standard 2 input boxes: Width & Depth
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = width1Input,
                            onValueChange = {
                                width1Input = it
                                width2Input = it
                            },
                            label = { Text("Plot Width (${unit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plot_length_input")
                        )
                        OutlinedTextField(
                            value = length1Input,
                            onValueChange = {
                                length1Input = it
                                length2Input = it
                            },
                            label = { Text("Plot Depth (${unit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plot_breadth_input")
                        )
                    }
                } else {
                    // 2 Width inputs + 2 Length inputs for irregular/trapezoidal plots
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Enter each side's measurement (e.g. 19 m on one side, 25 m on the other)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = width1Input,
                                onValueChange = { width1Input = it },
                                label = { Text("Width 1 (Front)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("width1_input")
                            )
                            OutlinedTextField(
                                value = width2Input,
                                onValueChange = { width2Input = it },
                                label = { Text("Width 2 (Rear)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("width2_input")
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = length1Input,
                                onValueChange = { length1Input = it },
                                label = { Text("Length 1 (Left)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("length1_input")
                            )
                            OutlinedTextField(
                                value = length2Input,
                                onValueChange = { length2Input = it },
                                label = { Text("Length 2 (Right)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("length2_input")
                            )
                        }
                    }
                }

                // 4. Number of Floors (Writeable text box + steppers)
                Text("Number of Floors", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val current = floorCountInput.toIntOrNull() ?: 1
                            if (current > 1) floorCountInput = (current - 1).toString()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease floors")
                    }

                    OutlinedTextField(
                        value = floorCountInput,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                floorCountInput = it
                            }
                        },
                        label = { Text("Floors") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("floor_count_input")
                    )

                    IconButton(
                        onClick = {
                            val current = floorCountInput.toIntOrNull() ?: 1
                            floorCountInput = (current + 1).coerceAtMost(30).toString()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase floors")
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w1 = width1Input.toFloatOrNull()
                    val w2 = width2Input.toFloatOrNull() ?: w1
                    val l1 = length1Input.toFloatOrNull()
                    val l2 = length2Input.toFloatOrNull() ?: l1
                    val floors = floorCountInput.toIntOrNull() ?: 1

                    if (w1 == null || w1 <= 0f || l1 == null || l1 <= 0f || w2 == null || w2 <= 0f || l2 == null || l2 <= 0f) {
                        errorMessage = "Please enter valid plot measurements."
                        return@Button
                    }
                    if (floors < 1) {
                        errorMessage = "Number of floors must be at least 1."
                        return@Button
                    }

                    val w1Meters = unit.fromDisplayValue(w1)
                    val w2Meters = unit.fromDisplayValue(w2)
                    val l1Meters = unit.fromDisplayValue(l1)
                    val l2Meters = unit.fromDisplayValue(l2)

                    onCreateProject(
                        projectName.trim(),
                        w1Meters,
                        w2Meters,
                        l1Meters,
                        l2Meters,
                        unit,
                        floors
                    )
                },
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
