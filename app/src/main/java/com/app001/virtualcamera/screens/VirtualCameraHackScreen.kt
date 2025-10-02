package com.app001.virtualcamera.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app001.virtualcamera.system.SystemVirtualCamera
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@Composable
fun VirtualCameraHackScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val systemVirtualCamera = remember { SystemVirtualCamera(context as android.app.Activity) }
    
    var isHackActive by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var selectedVideoPath by remember { mutableStateOf("") }
    var hackStatus by remember { mutableStateOf("Ready to install") }
    var showVideoPicker by remember { mutableStateOf(false) }
    var targetApps by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Check if hack is already active
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val isActive = systemVirtualCamera.isVirtualCameraHackActivePublic()
                withContext(Dispatchers.Main) {
                    isHackActive = isActive
                    hackStatus = if (isActive) "Virtual Camera Hack Active" else "Ready to install"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hackStatus = "Error checking status: ${e.message}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Section
        HeaderSection(
            isHackActive = isHackActive,
            hackStatus = hackStatus,
            onNavigateBack = onNavigateBack
        )
        
        // Main Control Panel
        MainControlPanel(
            isHackActive = isHackActive,
            isInstalling = isInstalling,
            selectedVideoPath = selectedVideoPath,
            onInstallHack = {
                isInstalling = true
                installVirtualCameraHack(context, systemVirtualCamera, selectedVideoPath) { success, message ->
                    isInstalling = false
                    isHackActive = success
                    hackStatus = message
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            onUninstallHack = {
                uninstallVirtualCameraHack(context, systemVirtualCamera) { success, message ->
                    isHackActive = !success
                    hackStatus = message
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            onSelectVideo = {
                showVideoPicker = true
            }
        )
        
        // Video Selection Section
        VideoSelectionSection(
            selectedVideoPath = selectedVideoPath,
            onSelectVideo = {
                showVideoPicker = true
            }
        )
        
        // Target Apps Section
        TargetAppsSection(
            targetApps = targetApps,
            onRefreshApps = {
                refreshTargetApps(context, systemVirtualCamera) { apps ->
                    targetApps = apps
                }
            }
        )
        
        // Advanced Options
        AdvancedOptionsSection(
            systemVirtualCamera = systemVirtualCamera
        )
        
        // Status Information
        StatusInformationSection(
            isHackActive = isHackActive,
            hackStatus = hackStatus
        )
    }
    
    // Video Picker Dialog
    if (showVideoPicker) {
        VideoPickerDialog(
            onDismiss = { showVideoPicker = false },
            onVideoSelected = { path ->
                selectedVideoPath = path
                showVideoPicker = false
                Toast.makeText(context, "Video selected: ${path.substringAfterLast("/")}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun HeaderSection(
    isHackActive: Boolean,
    hackStatus: String,
    onNavigateBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHackActive) Color(0xFF1B5E20) else Color(0xFF424242)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF333333), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            if (isHackActive) Color(0xFF4CAF50) else Color(0xFFFF5722),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHackActive) Icons.Default.Security else Icons.Default.VpnKey,
                        contentDescription = "Hack Status",
                        modifier = Modifier.size(30.dp),
                        tint = Color.White
                    )
                }
            }
            
            Text(
                text = "Virtual Camera Hack",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "KYC Bypass & Advanced Camera Replacement",
                fontSize = 16.sp,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )
            
            // Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (isHackActive) Color(0xFF4CAF50) else Color(0xFF757575),
                            CircleShape
                        )
                )
                Text(
                    text = hackStatus,
                    fontSize = 14.sp,
                    color = if (isHackActive) Color(0xFF4CAF50) else Color(0xFFB0B0B0),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MainControlPanel(
    isHackActive: Boolean,
    isInstalling: Boolean,
    selectedVideoPath: String,
    onInstallHack: () -> Unit,
    onUninstallHack: () -> Unit,
    onSelectVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Control Panel",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Primary Action Button
            Button(
                onClick = if (isHackActive) onUninstallHack else onInstallHack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHackActive) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                ),
                enabled = !isInstalling
            ) {
                if (isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Installing Hack...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = if (isHackActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isHackActive) "Stop Hack" else "Start Hack",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isHackActive) "Stop Virtual Camera Hack" else "Start Virtual Camera Hack",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            // Video Selection Button
            OutlinedButton(
                onClick = onSelectVideo,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF2196F3)
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3))
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Select Video",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedVideoPath.isEmpty()) "Select Replacement Video" else "Change Video",
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Selected Video Info
            if (selectedVideoPath.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Video Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Video: ${selectedVideoPath.substringAfterLast("/")}",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoSelectionSection(
    selectedVideoPath: String,
    onSelectVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Video Selection",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Video Selection",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Text(
                text = "Choose a video file to replace camera feed. Supported formats: MP4, AVI, MOV, MKV",
                fontSize = 14.sp,
                color = Color(0xFFB0B0B0)
            )
            
            // Quick Test Pattern Button
            Button(
                onClick = { /* Create test pattern */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Pattern,
                    contentDescription = "Test Pattern",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Test Pattern",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TargetAppsSection(
    targetApps: List<String>,
    onRefreshApps: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Target Apps",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Target Applications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                IconButton(
                    onClick = onRefreshApps,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF333333), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            if (targetApps.isEmpty()) {
                Text(
                    text = "No target applications detected. The hack will work system-wide.",
                    fontSize = 14.sp,
                    color = Color(0xFFB0B0B0)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(targetApps) { app ->
                        AppListItem(appName = app)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(appName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF333333)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = "App",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = appName,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Active",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AdvancedOptionsSection(
    systemVirtualCamera: SystemVirtualCamera
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Advanced Options",
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Advanced Options",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Advanced Hook Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Install advanced hooks */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F51B5)
                    )
                ) {
                    Text(
                        text = "Advanced Hooks",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
                
                Button(
                    onClick = { /* Install service hooks */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE91E63)
                    )
                ) {
                    Text(
                        text = "Service Hooks",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusInformationSection(
    isHackActive: Boolean,
    hackStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHackActive) Color(0xFF1B5E20) else Color(0xFF424242)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isHackActive) Icons.Default.Security else Icons.Default.Info,
                    contentDescription = "Status",
                    tint = if (isHackActive) Color(0xFF4CAF50) else Color(0xFF757575),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Status Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Text(
                text = if (isHackActive) {
                    "✅ Virtual Camera Hack is active and intercepting camera feeds system-wide. All camera applications will show your selected video instead of the real camera."
                } else {
                    "⏸️ Virtual Camera Hack is not active. Camera applications will show normal camera feed."
                },
                fontSize = 14.sp,
                color = Color(0xFFB0B0B0)
            )
            
            if (isHackActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Root access required for system-wide operation",
                        fontSize = 12.sp,
                        color = Color(0xFFFFC107)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPickerDialog(
    onDismiss: () -> Unit,
    onVideoSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Video",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose a video file to use as camera replacement:",
                    color = Color(0xFFB0B0B0)
                )
                
                // Sample video options
                listOf(
                    "Sample Video 1 (MP4)",
                    "Sample Video 2 (AVI)", 
                    "Test Pattern (Generated)",
                    "Custom Video File"
                ).forEach { option ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF333333)
                        ),
                        onClick = {
                            onVideoSelected(option)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Video",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = option,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF2196F3))
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

// Helper functions
private fun installVirtualCameraHack(
    context: Context,
    systemVirtualCamera: SystemVirtualCamera,
    videoPath: String,
    onResult: (Boolean, String) -> Unit
) {
    try {
        // Install the virtual camera hack
        val success = systemVirtualCamera.installVirtualCameraHackPublic()
        
        if (success && videoPath.isNotEmpty()) {
            // Load the video for hack
            val videoLoaded = systemVirtualCamera.loadVideoForHackPublic(videoPath)
            
            if (videoLoaded) {
                // Start the hack
                val hackStarted = systemVirtualCamera.startVirtualCameraHackPublic()
                
                if (hackStarted) {
                    onResult(true, "✅ Virtual Camera Hack installed and started successfully!")
                } else {
                    onResult(false, "❌ Failed to start Virtual Camera Hack")
                }
            } else {
                onResult(false, "❌ Failed to load video for hack")
            }
        } else if (success) {
            onResult(true, "✅ Virtual Camera Hack installed (no video selected)")
        } else {
            onResult(false, "❌ Failed to install Virtual Camera Hack")
        }
    } catch (e: Exception) {
        onResult(false, "❌ Error: ${e.message}")
    }
}

private fun uninstallVirtualCameraHack(
    context: Context,
    systemVirtualCamera: SystemVirtualCamera,
    onResult: (Boolean, String) -> Unit
) {
    try {
        systemVirtualCamera.uninstallVirtualCameraHackPublic()
        onResult(true, "✅ Virtual Camera Hack uninstalled successfully!")
    } catch (e: Exception) {
        onResult(false, "❌ Error: ${e.message}")
    }
}

private fun refreshTargetApps(
    context: Context,
    systemVirtualCamera: SystemVirtualCamera,
    onResult: (List<String>) -> Unit
) {
    try {
        // This would typically scan for camera apps
        val sampleApps = listOf(
            "com.android.camera",
            "com.google.camera",
            "com.samsung.camera",
            "com.oneplus.camera"
        )
        onResult(sampleApps)
    } catch (e: Exception) {
        onResult(emptyList())
    }
}
