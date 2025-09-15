package com.app001.virtualcamera.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app001.virtualcamera.system.SystemVirtualCamera
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigateToPreview: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var isRootAccessEnabled by remember { mutableStateOf(false) }
    var isCheckingRoot by remember { mutableStateOf(false) }
    var rootStatusMessage by remember { mutableStateOf("Checking root access...") }
    var showRootRequestDialog by remember { mutableStateOf(false) }
    var showForceRootRequest by remember { mutableStateOf(false) }
    
    // Initialize SystemVirtualCamera and check root status
    val systemVirtualCamera = remember { SystemVirtualCamera(context as android.app.Activity) }
    
    // Check root access when Home screen loads
    LaunchedEffect(Unit) {
        isCheckingRoot = true
        rootStatusMessage = "Checking root access..."
        
        try {
            val hasRoot = systemVirtualCamera.isDeviceRooted()
            isRootAccessEnabled = hasRoot
            rootStatusMessage = if (hasRoot) {
                "✅ Root access granted"
            } else {
                "❌ Root access required"
            }
            
            // If no root access, show request dialog
            if (!hasRoot) {
                delay(1000) // Wait a moment for UI to settle
                showRootRequestDialog = true
            }
        } catch (e: Exception) {
            rootStatusMessage = "❌ Error checking root access: ${e.message}"
            isRootAccessEnabled = false
            delay(1000)
            showRootRequestDialog = true
        } finally {
            isCheckingRoot = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Camera Icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Camera Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Color(0xFF4CAF50),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📷",
                    fontSize = 40.sp,
                    color = Color.White
                )
            }
            
            Text(
                text = "Replace Virtual Camera",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
            
            Text(
                text = "System-wide Virtual Camera Replacement",
                fontSize = 16.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                // Preview Video Button
                Button(
                    onClick = onNavigateToPreview,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview Video",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preview Video",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Settings Button
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Features Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E8)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                
                FeatureItem(
                    icon = Icons.Default.VideoLibrary,
                    title = "Video Preview",
                    description = "Preview your selected video with auto-play"
                )
                
                FeatureItem(
                    icon = Icons.Default.Camera,
                    title = "System Integration",
                    description = "Replace system camera across all apps"
                )
                
                FeatureItem(
                    icon = Icons.Default.Settings,
                    title = "Custom Settings",
                    description = "Configure your virtual camera preferences"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isRootAccessEnabled) Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Root Status",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRootAccessEnabled) Color(0xFF4CAF50) else Color(0xFFE65100)
                    )
                    
                    if (isCheckingRoot) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF4CAF50),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isRootAccessEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Root Status",
                            modifier = Modifier.size(20.dp),
                            tint = if (isRootAccessEnabled) Color(0xFF4CAF50) else Color(0xFFE65100)
                        )
                    }
                }
                
                Text(
                    text = rootStatusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isRootAccessEnabled) Color(0xFF4CAF50) else Color(0xFFE65100)
                )
                
                if (!isRootAccessEnabled) {
                    Text(
                        text = "⚠️ Root access required for system-wide functionality",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                } else {
                    Text(
                        text = "✅ GhostCam is ready to use!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
    
    // Root Request Dialog
    if (showRootRequestDialog) {
        RootRequestDialog(
            onDismiss = { showRootRequestDialog = false },
            onRequestRoot = {
                showRootRequestDialog = false
                requestRootAccess(context, systemVirtualCamera)
            },
            onForceRequest = {
                showRootRequestDialog = false
                showForceRootRequest = true
            }
        )
    }
    
    // Force Root Request Handler
    if (showForceRootRequest) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "🚀 Starting force root request...", Toast.LENGTH_SHORT).show()
            val forceResult = systemVirtualCamera.forceRootPermissionRequest()
            showForceRootRequest = false
            
            if (forceResult) {
                Toast.makeText(context, "✅ Force root request successful! GhostCam is ready.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "❌ Force root request failed. Please check your root manager.", Toast.LENGTH_LONG).show()
                showRootInstructionsDialog(context)
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF4CAF50)
        )
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
    }
}

// Root Request Dialog Composable
@Composable
private fun RootRequestDialog(
    onDismiss: () -> Unit,
    onRequestRoot: () -> Unit,
    onForceRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Security Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Color(0xFFE65100),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
                
                // Title
                Text(
                    text = "Root Access Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    textAlign = TextAlign.Center
                )
                
                // Description
                Text(
                    text = "GhostCam needs superuser permissions to replace the system camera and provide system-wide virtual camera functionality.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                
                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "To enable GhostCam:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        
                        Text(
                            text = "1. Open your root manager app (SuperSU, Magisk, etc.)\n2. Grant superuser permissions to GhostCam\n3. Return to this app and tap 'Request Root Access'\n4. GhostCam will be ready to use!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }
                
                // Warning
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFE65100)
                    )
                    Text(
                        text = "Root access is required for system-wide functionality",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF666666)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Text("Cancel")
                        }
                        
                        // Request Root Button
                        Button(
                            onClick = onRequestRoot,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE65100)
                            )
                        ) {
                            Text("Request")
                        }
                    }
                    
                    // Force Request Button
                    Button(
                        onClick = onForceRequest,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Force Request",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Force Root Request")
                    }
                }
            }
        }
    }
}

// Helper function to request root access
private fun requestRootAccess(context: Context, systemVirtualCamera: SystemVirtualCamera) {
    try {
        // First check if root is available
        val hasRoot = systemVirtualCamera.isDeviceRooted()
        
        if (hasRoot) {
            Toast.makeText(context, "✅ Root access granted! GhostCam is ready to use.", Toast.LENGTH_LONG).show()
        } else {
            // Programmatically request root access using multiple methods
            Toast.makeText(context, "🔐 Requesting root access...", Toast.LENGTH_SHORT).show()
            
            // Try programmatic root request
            val rootResult = systemVirtualCamera.programmaticallyRequestRootAccess()
            
            if (rootResult.success) {
                Toast.makeText(context, "✅ ${rootResult.message}", Toast.LENGTH_LONG).show()
            } else {
                // If programmatic request fails, try force request
                Toast.makeText(context, "🔄 Trying alternative root request methods...", Toast.LENGTH_SHORT).show()
                
                val forceResult = systemVirtualCamera.forceRootPermissionRequest()
                
                if (forceResult) {
                    Toast.makeText(context, "✅ Root access granted via force request!", Toast.LENGTH_LONG).show()
                } else {
                    // Show detailed instructions if all methods fail
                    showRootInstructionsDialog(context)
                }
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error requesting root access: ${e.message}", Toast.LENGTH_SHORT).show()
        showRootInstructionsDialog(context)
    }
}

// Helper function to attempt requesting superuser permissions
private fun requestSuperuserPermissions(context: Context, systemVirtualCamera: SystemVirtualCamera): Boolean {
    return try {
        // Try to request superuser permissions
        systemVirtualCamera.requestSuperuserPermissions()
    } catch (e: Exception) {
        false
    }
}

// Helper function to show detailed root instructions
private fun showRootInstructionsDialog(context: Context) {
    // Create a more detailed dialog with step-by-step instructions
    val instructions = """
        🔐 Root Access Required
        
        GhostCam needs superuser permissions to replace the system camera.
        
        To enable GhostCam:
        
        1. Open your root manager app:
           • SuperSU
           • Magisk Manager
           • KingRoot
           • Or any other root manager
        
        2. Look for "GhostCam" or "Virtual Camera" in the app list
        
        3. Grant "Allow" or "Grant" permissions
        
        4. Return to GhostCam and try again
        
        Note: If you don't have a root manager, you need to root your device first.
    """.trimIndent()
    
    Toast.makeText(context, instructions, Toast.LENGTH_LONG).show()
}
