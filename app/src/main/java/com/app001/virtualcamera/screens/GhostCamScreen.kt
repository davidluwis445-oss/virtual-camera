package com.app001.virtualcamera.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app001.virtualcamera.camera.VirtualCameraManager
import com.app001.virtualcamera.system.SystemVirtualCamera

/**
 * GhostCam-style Virtual Camera Screen
 * This screen shows how to use the virtual camera like GhostCam
 * where users can select the virtual camera as a separate device
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhostCamScreen(
    onNavigateBack: () -> Unit
) {
    var isVirtualCameraRegistered by remember { mutableStateOf(false) }
    var isVirtualCameraActive by remember { mutableStateOf(false) }
    var availableDevices by remember { mutableStateOf<List<VirtualCameraManager.CameraDeviceInfo>>(emptyList()) }
    var selectedVideoPath by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val systemVirtualCamera = remember { SystemVirtualCamera(context) }
    val virtualCameraManager = remember { VirtualCameraManager(context) }
    
    LaunchedEffect(Unit) {
        // Check initial state
        isVirtualCameraRegistered = systemVirtualCamera.isVirtualCameraHackActivePublic()
        availableDevices = virtualCameraManager.getAvailableCameraDevices()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎥 GhostCam Virtual Camera",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select virtual camera as a separate device in any app",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isVirtualCameraRegistered) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📊 Virtual Camera Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Registration:")
                    Text(
                        text = if (isVirtualCameraRegistered) "✅ Active" else "❌ Inactive",
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Feed:")
                    Text(
                        text = if (isVirtualCameraActive) "📹 Streaming" else "⏸️ Stopped",
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Available in Apps:")
                    Text(
                        text = if (isVirtualCameraRegistered) "✅ Yes" else "❌ No",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // How to Use Instructions
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📋 How to Use (GhostCam Style)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val steps = listOf(
                    "1. 📱 Ensure your device is rooted",
                    "2. 🎥 Register virtual camera below",
                    "3. 📹 Start virtual camera feed",
                    "4. 🔍 Open any camera app (TikTok, Telegram, etc.)",
                    "5. ⚙️ Go to camera settings/selection",
                    "6. 🎯 Select 'GhostCam Virtual Camera' from device list",
                    "7. 🎉 Enjoy virtual camera in any app!"
                )
                
                steps.forEach { step ->
                    Text(
                        text = step,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
        
        // Available Camera Devices
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📷 Available Camera Devices",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (availableDevices.isEmpty()) {
                    Text("No camera devices found")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(availableDevices) { device ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = device.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "ID: ${device.id} | ${device.facing}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (device.id == "virtual_camera_ghostcam") {
                                    Text(
                                        text = "🎥 VIRTUAL",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Video Path Input
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📹 Video File Path",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = selectedVideoPath,
                    onValueChange = { selectedVideoPath = it },
                    label = { Text("Video file path") },
                    placeholder = { Text("/sdcard/video.mp4") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Register Virtual Camera Button
            Button(
                onClick = {
                    isVirtualCameraRegistered = virtualCameraManager.registerVirtualCamera()
                    availableDevices = virtualCameraManager.getAvailableCameraDevices()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isVirtualCameraRegistered
            ) {
                Text("🎥 Register Virtual Camera")
            }
            
            // Start Virtual Camera Feed Button
            Button(
                onClick = {
                    if (selectedVideoPath.isNotEmpty()) {
                        isVirtualCameraActive = virtualCameraManager.startVirtualCameraFeed(selectedVideoPath)
                    } else {
                        // Use default video path
                        isVirtualCameraActive = systemVirtualCamera.startVirtualCameraFeed("/sdcard/sample_video.mp4")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isVirtualCameraRegistered && !isVirtualCameraActive
            ) {
                Text("📹 Start Virtual Camera Feed")
            }
            
            // Stop Virtual Camera Feed Button
            Button(
                onClick = {
                    virtualCameraManager.stopVirtualCameraFeed()
                    systemVirtualCamera.stopVirtualCameraFeed()
                    isVirtualCameraActive = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isVirtualCameraActive
            ) {
                Text("⏹️ Stop Virtual Camera Feed")
            }
            
            // Unregister Virtual Camera Button
            Button(
                onClick = {
                    virtualCameraManager.unregisterVirtualCamera()
                    systemVirtualCamera.uninstallSystemVirtualCamera()
                    isVirtualCameraRegistered = false
                    isVirtualCameraActive = false
                    availableDevices = virtualCameraManager.getAvailableCameraDevices()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isVirtualCameraRegistered
            ) {
                Text("🗑️ Unregister Virtual Camera")
            }
        }
        
        // Features Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🎉 GhostCam Features",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val features = listOf(
                    "🎥 Virtual Camera: Simulate virtual camera feed",
                    "📱 Works with All Apps: TikTok, Telegram, WhatsApp, Instagram",
                    "🔧 Requires Root: Device must be rooted",
                    "🤖 Android Compatible: Designed for Android devices",
                    "⚙️ Selectable Device: Choose virtual camera in app settings"
                )
                
                features.forEach { feature ->
                    Text(
                        text = feature,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Back Button
        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Back to Main Menu")
        }
    }
}
