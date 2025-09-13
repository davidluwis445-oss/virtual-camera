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

@Composable
fun AdvancedSetupScreen() {
    val context = LocalContext.current
    var isV4L2Available by remember { mutableStateOf(false) }
    var isSystemAppInstalled by remember { mutableStateOf(false) }
    var isDefaultCameraDisabled by remember { mutableStateOf(false) }
    var isV4L2Setup by remember { mutableStateOf(false) }
    var availableVideoDevices by remember { mutableStateOf<List<String>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Initialize SystemVirtualCamera
    val systemVirtualCamera = remember { SystemVirtualCamera(context as android.app.Activity) }
    
    // Check system status on load
    LaunchedEffect(Unit) {
        isProcessing = true
        delay(1000)
        
        try {
            // Check if device is rooted first
            val isRooted = systemVirtualCamera.isDeviceRooted()
            
            if (isRooted) {
                isV4L2Available = systemVirtualCamera.checkV4L2LoopbackAvailability()
                availableVideoDevices = systemVirtualCamera.getAvailableVideoDevices()
                isSystemAppInstalled = systemVirtualCamera.isVirtualCameraInstalled()
                isDefaultCameraDisabled = checkDefaultCameraStatus(systemVirtualCamera)
            } else {
                Toast.makeText(context, "⚠️ Root access required for advanced setup", Toast.LENGTH_LONG).show()
            }
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
            isAvailable = isV4L2Available,
            isProcessing = isProcessing,
            onSetup = {
                isProcessing = true
                try {
                    val success = systemVirtualCamera.setupV4L2LoopbackDevice()
                    isV4L2Setup = success
                    if (success) {
                        Toast.makeText(context, "✅ V4L2Loopback setup successful!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "❌ V4L2Loopback setup failed", Toast.LENGTH_LONG).show()
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
            isAvailable = systemVirtualCamera.isDeviceRooted(),
            isProcessing = isProcessing,
            onSetup = {
                isProcessing = true
                try {
                    Toast.makeText(context, "🔧 Installing as system app...", Toast.LENGTH_SHORT).show()
                    val installSuccess = systemVirtualCamera.installAsSystemApp()
                    
                    if (installSuccess) {
                        Toast.makeText(context, "🔧 Disabling default camera...", Toast.LENGTH_SHORT).show()
                        val disableSuccess = systemVirtualCamera.disableDefaultCamera()
                        
                        isSystemAppInstalled = installSuccess
                        isDefaultCameraDisabled = disableSuccess
                        
                        if (installSuccess && disableSuccess) {
                            Toast.makeText(context, "✅ Mock camera app setup successful!", Toast.LENGTH_LONG).show()
                        } else if (installSuccess) {
                            Toast.makeText(context, "⚠️ System app installed, but camera disabling failed", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "❌ System app installation failed", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "❌ System app installation failed", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error setting up mock camera: ${e.message}", Toast.LENGTH_SHORT).show()
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
            videoDevices = availableVideoDevices
        )
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
            
            if (videoDevices.isNotEmpty()) {
                Text(
                    text = "Video Devices: ${videoDevices.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1976D2)
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
