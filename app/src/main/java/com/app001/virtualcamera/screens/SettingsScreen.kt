package com.app001.virtualcamera.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app001.virtualcamera.system.SystemVirtualCamera
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var isWarningEnabled by remember { mutableStateOf(true) }
    var isSoundEnabled by remember { mutableStateOf(true) }
    var isEditorEnabled by remember { mutableStateOf(false) }
    var isRandomIPEnabled by remember { mutableStateOf(false) }
    var isRootAccessEnabled by remember { mutableStateOf(false) }
    var isCheckingRoot by remember { mutableStateOf(false) }
    var rootStatusMessage by remember { mutableStateOf("Checking root access...") }
    
    // Initialize SystemVirtualCamera and check root status
    val systemVirtualCamera = remember { SystemVirtualCamera(context as android.app.Activity) }
    
    // Check root access on first load
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
        } catch (e: Exception) {
            rootStatusMessage = "❌ Error checking root access: ${e.message}"
            isRootAccessEnabled = false
        } finally {
            isCheckingRoot = false
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
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )

        // Root Access Section
        SettingsCard(
            title = "System Access",
            icon = Icons.Default.Security,
            iconColor = if (isRootAccessEnabled) Color(0xFF4CAF50) else Color(0xFFE65100)
        ) {
            // Root Status Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Root Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isRootAccessEnabled) Color(0xFF4CAF50) else Color(0xFFE65100)
                    )
                    
                    Text(
                        text = rootStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRootAccessEnabled) Color(0xFF4CAF50) else Color(0xFFE65100),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Refresh/Request Button
                if (isCheckingRoot) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            // Request root access
                            requestRootAccess(
                                context = context,
                                systemVirtualCamera = systemVirtualCamera,
                                onRootStatusChange = { hasRoot, message ->
                                    isRootAccessEnabled = hasRoot
                                    rootStatusMessage = message
                                },
                                onCheckingChange = { checking ->
                                    isCheckingRoot = checking
                                }
                            )
                        }
                    ) {
                        Icon(
                            imageVector = if (isRootAccessEnabled) Icons.Default.Refresh else Icons.Default.Security,
                            contentDescription = if (isRootAccessEnabled) "Refresh" else "Request Root",
                            tint = if (isRootAccessEnabled) Color(0xFF4CAF50) else Color(0xFFE65100)
                        )
                    }
                }
            }
            
            // Root Access Information
            if (!isRootAccessEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ Root Access Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "GhostCam requires root access to replace the system camera. Please:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "1. Ensure your device is rooted\n2. Grant superuser permissions\n3. Tap the security icon to request access",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "✅ Root Access Granted",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "System-wide camera replacement is now available. You can install and use GhostCam features.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }


        SettingsCard(
            title = "Audio & Video",
            icon = Icons.Default.VolumeUp,
            iconColor = Color(0xFF2196F3)
        ) {
            SettingsSwitch(
                title = "Sound Effects",
                subtitle = "Play sounds for button interactions",
                checked = isSoundEnabled,
                onCheckedChange = { isSoundEnabled = it }
            )
        }


        SettingsCard(
            title = "Interface",
            icon = Icons.Default.Palette,
            iconColor = Color(0xFF9C27B0)
        ) {
            SettingsSwitch(
                title = "Warning Messages",
                subtitle = "Show warning dialogs for important actions",
                checked = isWarningEnabled,
                onCheckedChange = { isWarningEnabled = it }
            )
            
            SettingsSwitch(
                title = "Editor Mode",
                subtitle = "Enable advanced video editing features",
                checked = isEditorEnabled,
                onCheckedChange = { isEditorEnabled = it }
            )
        }


        SettingsCard(
            title = "Advanced",
            icon = Icons.Default.Build,
            iconColor = Color(0xFF607D8B)
        ) {
            SettingsSwitch(
                title = "Random IP-MAC",
                subtitle = "Randomize network identifiers for privacy",
                checked = isRandomIPEnabled,
                onCheckedChange = { isRandomIPEnabled = it }
            )
        }

        // App Information
        SettingsCard(
            title = "App Information",
            icon = Icons.Default.Info,
            iconColor = Color(0xFF795548)
        ) {
            SettingsItem(
                title = "Version",
                subtitle = "1.0.0",
                icon = Icons.Default.Update
            )
            
            SettingsItem(
                title = "Build Type",
                subtitle = "Debug",
                icon = Icons.Default.Code
            )
            
            SettingsItem(
                title = "Target SDK",
                subtitle = "API 36",
                icon = Icons.Default.PhoneAndroid
            )
        }


        SettingsCard(
            title = "Actions",
            icon = Icons.Default.Settings,
            iconColor = Color(0xFF4CAF50)
        ) {
            SettingsButton(
                title = "Reset to Defaults",
                subtitle = "Restore all settings to default values",
                icon = Icons.Default.Restore,
                onClick = {
                    isWarningEnabled = true
                    isSoundEnabled = true
                    isEditorEnabled = false
                    isRandomIPEnabled = false
                }
            )
            
            SettingsButton(
                title = "Clear Cache",
                subtitle = "Clear temporary files and cache",
                icon = Icons.Default.Delete,
                onClick = { /* Clear cache logic */ }
            )
            
            SettingsButton(
                title = "Export Settings",
                subtitle = "Export current settings to file",
                icon = Icons.Default.FileDownload,
                onClick = { /* Export settings logic */ }
            )
        }


        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Replace Virtual Camera",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "System-wide virtual camera replacement for Android devices. Requires root access for full functionality.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "© 2024 Replace Virtual Camera. All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Card Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Card Content
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) Color(0xFF333333) else Color(0xFF999999)
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4CAF50),
                checkedTrackColor = Color(0xFF81C784),
                uncheckedThumbColor = Color(0xFFBDBDBD),
                uncheckedTrackColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF666666)
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun SettingsButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF666666)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333)
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Action",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF999999)
            )
        }
    }
}

// Helper function to request root access
private fun requestRootAccess(
    context: Context,
    systemVirtualCamera: SystemVirtualCamera,
    onRootStatusChange: (Boolean, String) -> Unit,
    onCheckingChange: (Boolean) -> Unit
) {
    onCheckingChange(true)
    
    try {
        // Try to check root access
        val hasRoot = systemVirtualCamera.isDeviceRooted()
        
        if (hasRoot) {
            // Root access is available
            onRootStatusChange(true, "✅ Root access granted")
            Toast.makeText(context, "Root access granted! GhostCam is ready to use.", Toast.LENGTH_LONG).show()
        } else {
            // No root access - try to request it
            onRootStatusChange(false, "❌ Root access required")
            Toast.makeText(context, "Root access required. Please grant superuser permissions.", Toast.LENGTH_LONG).show()
            
            // You could add additional root request logic here
            // For example, showing a dialog with instructions
            showRootRequestDialog(context)
        }
    } catch (e: Exception) {
        onRootStatusChange(false, "❌ Error: ${e.message}")
        Toast.makeText(context, "Error checking root access: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        onCheckingChange(false)
    }
}

// Helper function to show root request dialog
private fun showRootRequestDialog(context: Context) {
    // This could be expanded to show a proper dialog
    // For now, we'll just show a toast with instructions
    Toast.makeText(
        context,
        "To enable GhostCam:\n1. Open your root manager app\n2. Grant superuser permissions to GhostCam\n3. Return to this app and tap the security icon again",
        Toast.LENGTH_LONG
    ).show()
}
