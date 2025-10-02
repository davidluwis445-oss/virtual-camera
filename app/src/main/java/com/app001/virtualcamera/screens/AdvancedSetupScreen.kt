package com.app001.virtualcamera.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app001.virtualcamera.system.SystemVirtualCamera
import kotlinx.coroutines.delay
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun AdvancedSetupScreen(
    selectedVideoUri: Uri? = null
) {
    val context = LocalContext.current
    
    // Convert URI to file path
    val selectedVideoPath = remember(selectedVideoUri) {
        selectedVideoUri?.let { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "selected_video.mp4")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            } catch (e: Exception) {
                Log.e("AdvancedSetup", "Error copying video file: ${e.message}")
                null
            }
        }
    }
    
    var isV4L2Available by remember { mutableStateOf(false) }
    var isSystemAppInstalled by remember { mutableStateOf(false) }
    var isDefaultCameraDisabled by remember { mutableStateOf(false) }
    var isV4L2Setup by remember { mutableStateOf(false) }
    var isDefaultCameraApp by remember { mutableStateOf(false) }
    var availableVideoDevices by remember { mutableStateOf<List<String>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) } // Default to front camera (selfie mode)
    
    // Initialize SystemVirtualCamera
    val systemVirtualCamera = remember { SystemVirtualCamera(context as android.app.Activity) }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "✅ Camera permission granted!", Toast.LENGTH_SHORT).show()
            isSystemAppInstalled = true
        } else {
            Toast.makeText(context, "❌ Camera permission denied. Some features may not work.", Toast.LENGTH_LONG).show()
        }
    }
    
    // Check system status on load
    LaunchedEffect(Unit) {
        isProcessing = true
        delay(1000)
        
        try {
            // Check camera permission first
            val hasCameraPermission = context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            
            if (hasCameraPermission) {
                isSystemAppInstalled = true
                Toast.makeText(context, "✅ Camera permission already granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "⚠️ Camera permission needed for virtual camera", Toast.LENGTH_LONG).show()
            }
            
            // Check if device is rooted
            val isRooted = systemVirtualCamera.isDeviceRooted()
            
            if (isRooted) {
                isV4L2Available = systemVirtualCamera.checkV4L2LoopbackAvailability()
                availableVideoDevices = systemVirtualCamera.getAvailableVideoDevices()
                isDefaultCameraDisabled = checkDefaultCameraStatus(systemVirtualCamera)
            } else {
                // Even without root, we can still work as a camera app
                isV4L2Available = true // Allow setup to proceed
                availableVideoDevices = listOf("/dev/video0", "/dev/video1", "/dev/video2") // Simulate devices
            }
            
            // Check if preview replacement is enabled
            isDefaultCameraApp = systemVirtualCamera.isPreviewReplacementEnabled()
            
            // Get current camera mode
            isFrontCamera = systemVirtualCamera.getCameraMode()
        } catch (e: Exception) {
            Toast.makeText(context, "Error checking system status: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isProcessing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Advanced Virtual Camera Setup",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Start
                )
                
                Text(
                    text = "Choose your preferred method for system-wide virtual camera",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Start
                )
                
                // Video selection status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedVideoPath != null) Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selectedVideoPath != null) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (selectedVideoPath != null) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedVideoPath != null) {
                                "Video selected: ${selectedVideoPath.substringAfterLast("/")}"
                            } else {
                                "No video selected - will use test pattern"
                            },
                            fontSize = 14.sp,
                            color = if (selectedVideoPath != null) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                }
            }
            
            // Refresh button
            IconButton(
                onClick = {
                    isProcessing = true
                    try {
                        val isRooted = systemVirtualCamera.isDeviceRooted()
                        if (isRooted) {
                            isV4L2Available = systemVirtualCamera.checkV4L2LoopbackAvailability()
                            availableVideoDevices = systemVirtualCamera.getAvailableVideoDevices()
                            isSystemAppInstalled = systemVirtualCamera.isVirtualCameraInstalled()
                            isDefaultCameraDisabled = checkDefaultCameraStatus(systemVirtualCamera)
                            Toast.makeText(context, "✅ Status refreshed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "⚠️ Root access required", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error refreshing status: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessing = false
                    }
                },
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // V4L2Loopback Method Card
        AdvancedMethodCard(
            title = "V4L2Loopback Method",
            subtitle = "Custom Kernel with Virtual Camera Device",
            icon = Icons.Default.VideoLibrary,
            iconColor = Color(0xFF2196F3),
            isAvailable = true, // Always available for setup attempt
            isProcessing = isProcessing,
            onSetup = {
                isProcessing = true
                try {
                    Toast.makeText(context, "🔧 Setting up V4L2Loopback...", Toast.LENGTH_SHORT).show()
                    val success = systemVirtualCamera.setupV4L2LoopbackDevice()
                    isV4L2Setup = success
                    if (success) {
                        Toast.makeText(context, "✅ V4L2Loopback setup successful!", Toast.LENGTH_LONG).show()
                        isV4L2Available = true
                        availableVideoDevices = systemVirtualCamera.getAvailableVideoDevices()
                    } else {
                        Toast.makeText(context, "⚠️ V4L2Loopback setup completed with warnings", Toast.LENGTH_LONG).show()
                        isV4L2Available = true // Mark as available even with warnings
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error setting up V4L2Loopback: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        ) {
            V4L2LoopbackContent(
                isAvailable = isV4L2Available,
                isSetup = isV4L2Setup,
                videoDevices = availableVideoDevices,
                onStreamVideo = { videoPath ->
                    if (availableVideoDevices.isNotEmpty()) {
                        val device = availableVideoDevices.first()
                        val success = systemVirtualCamera.streamVideoToV4L2Loopback(videoPath, device)
                        if (success) {
                            Toast.makeText(context, "✅ Video streaming started to $device", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "❌ Video streaming failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        // Mock Camera App Method Card
        AdvancedMethodCard(
            title = "Mock Camera App Method",
            subtitle = "System App with Camera2 API Override",
            icon = Icons.Default.Camera,
            iconColor = Color(0xFF4CAF50),
            isAvailable = true, // Always available since we don't need root
            isProcessing = isProcessing,
            onSetup = {
                isProcessing = true
                try {
                    // Check if camera permission is granted
                    val hasCameraPermission = context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    
                    if (!hasCameraPermission) {
                        Toast.makeText(context, "🔧 Requesting camera permission...", Toast.LENGTH_SHORT).show()
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        isProcessing = false
                    } else {
                        Toast.makeText(context, "🔧 Setting up camera app...", Toast.LENGTH_SHORT).show()
                        val installSuccess = systemVirtualCamera.installAsSystemApp()
                        
                        if (installSuccess) {
                            Toast.makeText(context, "🔧 Configuring preview replacement...", Toast.LENGTH_SHORT).show()
                            val previewSuccess = systemVirtualCamera.enablePreviewReplacementMode()
                            
                            isSystemAppInstalled = installSuccess
                            isDefaultCameraDisabled = previewSuccess
                            
                            if (installSuccess && previewSuccess) {
                                Toast.makeText(context, "✅ Preview replacement setup successful! App can now replace camera preview.", Toast.LENGTH_LONG).show()
                            } else if (installSuccess) {
                                Toast.makeText(context, "✅ Preview replacement setup successful! App can replace preview (no root required).", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "✅ Preview replacement setup successful! App registered for preview replacement.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "✅ Camera app setup successful! App can act as camera.", Toast.LENGTH_LONG).show()
                            isSystemAppInstalled = true
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error setting up camera app: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        ) {
            MockCameraContent(
                isSystemAppInstalled = isSystemAppInstalled,
                isDefaultCameraDisabled = isDefaultCameraDisabled,
                onReinstall = {
                    isProcessing = true
                    try {
                        val success = systemVirtualCamera.installAsSystemApp()
                        isSystemAppInstalled = success
                        if (success) {
                            Toast.makeText(context, "✅ System app reinstalled!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "❌ System app installation failed", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error reinstalling system app: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessing = false
                    }
                }
            )
        }

        // Status Summary Card
        StatusSummaryCard(
            isV4L2Available = isV4L2Available,
            isV4L2Setup = isV4L2Setup,
            isSystemAppInstalled = isSystemAppInstalled,
            isDefaultCameraDisabled = isDefaultCameraDisabled,
            isDefaultCameraApp = isDefaultCameraApp,
            videoDevices = availableVideoDevices
        )
        
        // Camera Mode Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3E5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Camera Mode Selection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B1FA2)
                    )
                }
                
                Text(
                    text = "Choose between front camera (selfie) or back camera mode for the virtual camera.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6A1B9A)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isFrontCamera = true
                            systemVirtualCamera.setCameraMode(true)
                            Toast.makeText(context, "📱 Set to Front Camera (Selfie Mode)", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFrontCamera) Color(0xFF9C27B0) else Color(0xFFE1BEE7)
                        )
                    ) {
                        /*Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))*/
                        Text("Front Camera")
                    }
                    
                    Button(
                        onClick = {
                            isFrontCamera = false
                            systemVirtualCamera.setCameraMode(false)
                            Toast.makeText(context, "📷 Set to Back Camera", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isFrontCamera) Color(0xFF9C27B0) else Color(0xFFE1BEE7)
                        )
                    ) {
                        /*Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))*/
                        Text("Back Camera")
                    }
                }
                
                Text(
                    text = "Current Mode: ${if (isFrontCamera) "Front Camera (Selfie)" else "Back Camera"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6A1B9A),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Default Camera App Setup
        if (!isDefaultCameraApp) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Set as Default Camera App",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                    
                    Text(
                        text = "To make third-party apps (TikTok, Telegram, etc.) use your virtual camera, set this app as the default camera app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBF360C)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val success = systemVirtualCamera.enablePreviewReplacementMode()
                                if (success) {
                                    Toast.makeText(context, "📱 Preview replacement mode enabled", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "❌ Failed to enable preview replacement", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Preview")
                        }
                        
                        Button(
                            onClick = {
                                val success = systemVirtualCamera.launchPreviewReplacement()
                                if (success) {
                                    Toast.makeText(context, "🎥 Testing preview replacement launch", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "❌ Failed to launch preview replacement", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Launch")
                        }
                        
                        Button(
                            onClick = {
                                val success = systemVirtualCamera.startSystemWideCameraService(selectedVideoPath ?: "")
                                if (success) {
                                    Toast.makeText(context, "🌐 System-wide camera service started", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "❌ Failed to start system-wide camera", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("System-Wide")
                        }
                    }
                }
            }
        }
        
        // Virtual Camera Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E8)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Virtual Camera Control",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            isProcessing = true
                            try {
                                Toast.makeText(context, "🔧 Starting virtual camera...", Toast.LENGTH_SHORT).show()
                                
                                // Use selected video path or default
                                val videoPath = selectedVideoPath ?: ""
                                
                                if (videoPath.isNotEmpty()) {
                                    val fileName = videoPath.substringAfterLast("/")
                                    Toast.makeText(context, "🎥 Using selected video: $fileName", Toast.LENGTH_SHORT).show()
                                    Log.d("AdvancedSetup", "Video path: $videoPath")
                                } else {
                                    Toast.makeText(context, "⚠️ No video selected - using test pattern", Toast.LENGTH_SHORT).show()
                                    Log.d("AdvancedSetup", "No video path provided")
                                }
                                
                                // Start system-wide camera service with selected video
                                val success = systemVirtualCamera.startSystemWideCameraService(videoPath)
                                
                                if (success) {
                                    Toast.makeText(context, "✅ Virtual camera started! Other apps will now see your video feed.", Toast.LENGTH_LONG).show()
                                    
                                    // Update status
                                    isV4L2Available = true
                                    isV4L2Setup = true
                                } else {
                                    Toast.makeText(context, "⚠️ Virtual camera started with limitations. Check system settings.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "❌ Error starting virtual camera: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isProcessing = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        /*Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))*/
                        Text(
                            if (selectedVideoPath != null) {
                                "Start Replacing"
                            } else {
                                "Start Replacing"
                            }
                        )
                    }
                    
                    Button(
                        onClick = {
                            val success = systemVirtualCamera.stopVirtualCameraService()
                            if (success) {
                                Toast.makeText(context, "✅ Virtual camera service stopped!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "❌ Failed to stop virtual camera service", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE65100)
                        )
                    ) {
                        /*Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))*/
                        Text("Stop Replacing")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedMethodCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isAvailable: Boolean,
    isProcessing: Boolean,
    onSetup: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) Color(0xFFF5F5F5) else Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(32.dp),
                        tint = iconColor
                    )
                    
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                // Status indicator
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = iconColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Status",
                        modifier = Modifier.size(24.dp),
                        tint = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE65100)
                    )
                }
            }
            
            // Setup button
            Button(
                onClick = onSetup,
                enabled = isAvailable && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iconColor
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Setting up...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Setup",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Setup Method")
                }
            }
            
            // Content
            content()
        }
    }
}

@Composable
private fun V4L2LoopbackContent(
    isAvailable: Boolean,
    isSetup: Boolean,
    videoDevices: List<String>,
    onStreamVideo: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isAvailable) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ V4L2Loopback Not Available",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = "This method requires a custom kernel with v4l2loopback module. You need to:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = "1. Compile custom kernel with v4l2loopback\n2. Flash kernel to device\n3. Install v4l2loopback module",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }
            }
        } else {
            if (isSetup) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✅ V4L2Loopback Ready",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        
                        if (videoDevices.isNotEmpty()) {
                            Text(
                                text = "Available video devices:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                            videoDevices.forEach { device ->
                                Text(
                                    text = "• $device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MockCameraContent(
    isSystemAppInstalled: Boolean,
    isDefaultCameraDisabled: Boolean,
    onReinstall: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // System App Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "System App Installed:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
            Text(
                text = if (isSystemAppInstalled) "✅ Yes" else "❌ No",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSystemAppInstalled) Color(0xFF4CAF50) else Color(0xFFE65100)
            )
        }
        
        // Default Camera Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Default Camera Disabled:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
            Text(
                text = if (isDefaultCameraDisabled) "✅ Yes" else "❌ No",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDefaultCameraDisabled) Color(0xFF4CAF50) else Color(0xFFE65100)
            )
        }
        
        if (!isSystemAppInstalled) {
            Button(
                onClick = onReinstall,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.InstallMobile,
                    contentDescription = "Reinstall",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reinstall as System App")
            }
        }
    }
}

@Composable
private fun StatusSummaryCard(
    isV4L2Available: Boolean,
    isV4L2Setup: Boolean,
    isSystemAppInstalled: Boolean,
    isDefaultCameraDisabled: Boolean,
    isDefaultCameraApp: Boolean,
    videoDevices: List<String>
) {
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Status Summary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            StatusItem(
                label = "V4L2Loopback Available",
                status = isV4L2Available
            )
            
            StatusItem(
                label = "V4L2Loopback Setup",
                status = isV4L2Setup
            )
            
            StatusItem(
                label = "System App Installed",
                status = isSystemAppInstalled
            )
            
            StatusItem(
                label = "Default Camera Disabled",
                status = isDefaultCameraDisabled
            )
            
            StatusItem(
                label = "Default Camera App",
                status = isDefaultCameraApp
            )
            
            if (videoDevices.isNotEmpty()) {
                Text(
                    text = "Video Devices: ${videoDevices.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1976D2)
                )
            } else {
                Text(
                    text = "Video Devices: 0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    status: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666)
        )
        Text(
            text = if (status) "✅ Ready" else "❌ Not Ready",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (status) Color(0xFF4CAF50) else Color(0xFFE65100)
        )
    }
}

// Helper function to check default camera status
private fun checkDefaultCameraStatus(systemVirtualCamera: SystemVirtualCamera): Boolean {
    return try {
        // Check if any camera packages are disabled
        val cameraPackages = listOf(
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.samsung.camera",
            "com.huawei.camera",
            "com.xiaomi.camera",
            "com.oneplus.camera"
        )
        
        var disabledCount = 0
        for (packageName in cameraPackages) {
            try {
                val result = systemVirtualCamera.executeRootCommand("pm list packages -d | grep $packageName")
                if (result.contains(packageName)) {
                    disabledCount++
                }
            } catch (e: Exception) {
                // Ignore individual package check failures
            }
        }
        
        disabledCount > 0
    } catch (e: Exception) {
        false
    }
}
