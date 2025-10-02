package com.app001.virtualcamera.screens

import android.content.Context
import android.content.Intent
import android.icu.util.TimeZone
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app001.virtualcamera.camera.SimpleCameraSurfaceInjection
import com.app001.virtualcamera.camera.SystemWideCameraHook
import com.app001.virtualcamera.system.SystemVirtualCamera
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var isRootAccessEnabled by remember { mutableStateOf(false) }
    var isCheckingRoot by remember { mutableStateOf(false) }
    var rootStatusMessage by remember { mutableStateOf("Checking root access...") }
    var showRootRequestDialog by remember { mutableStateOf(false) }
    var showForceRootRequest by remember { mutableStateOf(false) }
    var showCameraRestoreDialog by remember { mutableStateOf(false) }

    var enabledCameraApps by remember { mutableStateOf<List<String>>(emptyList()) }
    var disabledCameraApps by remember { mutableStateOf<List<String>>(emptyList()) }

    var isCheckingCameraApps by remember { mutableStateOf(false) }

    val systemVirtualCamera = remember { SystemVirtualCamera(context as android.app.Activity) }

    LaunchedEffect(Unit) {
        isCheckingRoot = true
        rootStatusMessage = "Checking root access..."

        withContext(Dispatchers.IO) {
            try {
                val hasRoot = systemVirtualCamera.isDeviceRooted()
                
                withContext(Dispatchers.Main) {
                    isRootAccessEnabled = hasRoot
                    rootStatusMessage = if (hasRoot) {
                        "Root access granted"
                    } else {
                        "Root access required"
                    }

                    if (!hasRoot) {
                        delay(1000)
                        showRootRequestDialog = true
                    } else {
                        checkCameraAppStatusAsync(systemVirtualCamera, enabledCameraApps, disabledCameraApps) { enabled, disabled ->
                            enabledCameraApps = enabled
                            disabledCameraApps = disabled
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    rootStatusMessage = "❌ Error checking root access: ${e.message}"
                    isRootAccessEnabled = false
                    delay(1000)
                    showRootRequestDialog = true
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isCheckingRoot = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {

        HeaderSection()

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            ReplaceCameraCard(
                isRootAccessEnabled = isRootAccessEnabled,
                isCheckingRoot = isCheckingRoot,
                rootStatusMessage = rootStatusMessage,
                onReplaceCamera = {
                    setupVirtualCameraReplacement(context, systemVirtualCamera) { success, message ->
                        if (success) {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "$message", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onResetCamera = {
                    restoreDefaultCamera(context, systemVirtualCamera) { enabled, disabled ->
                        enabledCameraApps = enabled
                        disabledCameraApps = disabled
                    }
                }
            )

            PlayingVideoCard()

            PreviewCard()
            

            GhostCamCard()
            
        }

        VersionInfo()
    }

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

    if (showForceRootRequest) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Starting force root request...", Toast.LENGTH_SHORT).show()
            withContext(Dispatchers.IO) {
                val forceResult = systemVirtualCamera.forceRootPermissionRequest()
                
                withContext(Dispatchers.Main) {
                    showForceRootRequest = false
                    
                    if (forceResult) {
                        Toast.makeText(context, "Force root request successful! GhostCam is ready.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Force root request failed. Please check your root manager.", Toast.LENGTH_LONG).show()
                        showRootInstructionsDialog(context)
                    }
                }
            }
        }
    }

    if (showCameraRestoreDialog) {
        CameraRestoreDialog(
            enabledApps = enabledCameraApps,
            disabledApps = disabledCameraApps,
            onDismiss = { showCameraRestoreDialog = false },
            onRestore = {
                showCameraRestoreDialog = false
                restoreDefaultCamera(context, systemVirtualCamera) { enabled, disabled ->
                    enabledCameraApps = enabled
                    disabledCameraApps = disabled
                }
            },
            onRefresh = {
                checkCameraAppStatusAsync(systemVirtualCamera, enabledCameraApps, disabledCameraApps) { enabled, disabled ->
                    enabledCameraApps = enabled
                    disabledCameraApps = disabled
                }
            }
        )
    }
}

@Composable
private fun HeaderSection() {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2196F3))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Virtual Camera",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = sdf.format(Date()),
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ReplaceCameraCard(
    isRootAccessEnabled: Boolean,
    isCheckingRoot: Boolean,
    rootStatusMessage: String,
    onReplaceCamera: () -> Unit,
    onResetCamera: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "1. Replace Camera",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Text(
                text = if (isRootAccessEnabled) "success replace camera" else "failed replace camera",
                fontSize = 14.sp,
                color = if (isRootAccessEnabled) Color(0xFF4CAF50) else Color(0xFFE65100),
                fontWeight = FontWeight.Medium
            )

            if (isCheckingRoot) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF2196F3),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Processing...",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onReplaceCamera,
                    modifier = Modifier.weight(1f),
                    enabled = !isCheckingRoot,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text(
                        text = "REPLACE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                OutlinedButton(
                    onClick = onResetCamera,
                    modifier = Modifier.weight(1f),
                    enabled = !isCheckingRoot,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF666666)
                    )
                ) {
                    Text(
                        text = "RESET",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayingVideoCard(
) {
    val context = LocalContext.current
    var selectedVideo by remember { mutableStateOf("Sample Video 1 (MP4)") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var customVideos by remember { mutableStateOf<List<Pair<String, Uri>>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val fileName = try {
                context.contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: "Selected Video"
            } catch (e: Exception) {
                "Selected Video ${customVideos.size + 1}"
            }

            val videoEntry = Pair(fileName, selectedUri)
            if (!customVideos.any { it.second == selectedUri }) {
                customVideos = customVideos + videoEntry
            }

            selectedVideo = fileName
            selectedVideoUri = selectedUri
            expanded = false
        }
    }

    
    val videoOptions = customVideos.map { it.first } + "Browse for Video..."
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "2. Play Video",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            

            Text(
                text = "To use the rtmp function, please open the floating window and click 📁",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
            
            Text(
                text = "Choose from built-in samples or select 'Browse for Video...' to pick from your device storage",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )

            selectedVideoUri?.let { uri ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓ Custom video selected: $selectedVideo",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(
                        onClick = {
                            selectedVideo = "Select Video"
                            selectedVideoUri = null
                        }
                    ) {
                        Text(
                            text = "Clear",
                            fontSize = 10.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video Selection Dropdown
                OutlinedTextField(
                    value = selectedVideo,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Video Selection") }
                )

                OutlinedButton(
                    onClick = { videoPickerLauncher.launch("video/*") },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Browse Videos",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
            

            if (expanded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column {
                        videoOptions.forEach { option ->
                            TextButton(
                                onClick = {
                                    if (option == "Browse for Video...") {
                                        // Launch video picker
                                        videoPickerLauncher.launch("video/*")
                                    } else {
                                        selectedVideo = option
                                        // Update selectedVideoUri for custom videos
                                        selectedVideoUri = customVideos.find { it.first == option }?.second
                                        expanded = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                    if (option == "Browse for Video...") {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Browse",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            
            // Player Status - Simple Camera Surface Video Injection Status
            val surfaceInjection = remember { SimpleCameraSurfaceInjection.instance }
            val systemHook = remember { SystemWideCameraHook.instance }
            var surfaceInjectionStatus by remember { mutableStateOf("Ready to inject video") }
            var surfaceInjectionActive by remember { mutableStateOf(false) }
            
            // Check simple camera surface injection status (LIGHTWEIGHT - NO ANR)
            LaunchedEffect(isPlaying) {
                // Run lightweight status checking in background
                withContext(Dispatchers.IO) {
                    while (true) {
                        try {
                            // Lightweight status checks
                            val surfaceActive = surfaceInjection.isCameraSurfaceInjectionActive()
                            val systemActive = try {
                                systemHook.isSystemWideCameraHookActive()
                            } catch (e: Exception) {
                                false // Don't fail if system hook check fails
                            }
                            
                            val active = surfaceActive || systemActive
                            
                            val status = when {
                                surfaceActive && selectedVideoUri != null -> {
                                    val videoName = selectedVideo.takeIf { it != "Sample Video 1 (MP4)" } ?: "Custom Video"
                                    "🎬 \"$videoName\" READY FOR CAMERA SURFACES!"
                                }
                                surfaceActive -> "📹 Video injection ready - Open camera apps to test"
                                systemActive -> "🎯 System hook active - Camera monitoring"
                                isPlaying -> "▶️ Playing - Open TikTok/Instagram to see video"
                                else -> "📱 Ready to inject video into camera surfaces"
                            }
                            
                            // Update UI on main thread
                            withContext(Dispatchers.Main) {
                                surfaceInjectionActive = active
                                surfaceInjectionStatus = status
                            }
                            
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                surfaceInjectionStatus = "📱 Video injection ready (status check error)"
                            }
                        }
                        
                        delay(5000) // Check every 5 seconds (prevent ANR)
                    }
                }
            }
            
            Text(
                text = surfaceInjectionStatus,
                fontSize = 12.sp,
                color = if (surfaceInjectionActive) Color(0xFF4CAF50) else if (isPlaying) Color(0xFFFF9800) else Color(0xFFE65100),
                fontWeight = FontWeight.Medium
            )

            Text(
                text = if (isPlaying) "player is running" else "player is stopped",
                fontSize = 12.sp,
                color = if (isPlaying) Color(0xFF4CAF50) else Color(0xFFE65100),
                fontWeight = FontWeight.Normal
            )
            

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        if (isProcessing) return@Button

                        isPlaying = true
                        isProcessing = true

                        Toast.makeText(context, "🚀 Starting camera surface video injection...", Toast.LENGTH_SHORT).show()

                        GlobalScope.launch(Dispatchers.IO) {
                            try {

                                val surfaceInjection = SimpleCameraSurfaceInjection.instance

                                val success = selectedVideoUri?.let { uri ->
                                    surfaceInjection.setupCompleteCameraSurfaceInjection(context, uri)
                                } ?: run {
                                    val defaultVideoPath = "android.resource://${context.packageName}/raw/sample_video_tt"
                                    surfaceInjection.setupCompleteCameraSurfaceInjection(context, defaultVideoPath)
                                }
                                
                                if (success) {

                                    val systemHook = SystemWideCameraHook.instance
                                    val videoToLoad = selectedVideoUri?.toString() ?: "android.resource://${context.packageName}/raw/sample_video_tt"
                                    systemHook.setupCompleteSystemWideVirtualCamera(context, videoToLoad)
                                    

                                    withContext(Dispatchers.Main) {
                                        val serviceIntent = Intent(context, com.app001.virtualcamera.service.SystemWideVirtualCameraService::class.java).apply {
                                            action = com.app001.virtualcamera.service.SystemWideVirtualCameraService.ACTION_START_SYSTEM_CAMERA
                                            putExtra(com.app001.virtualcamera.service.SystemWideVirtualCameraService.EXTRA_VIDEO_PATH, videoToLoad)
                                        }
                                        context.startService(serviceIntent)

                                        val intent = Intent(context, com.app001.virtualcamera.camera.SimpleVirtualCameraActivity::class.java).apply {
                                            putExtra(com.app001.virtualcamera.camera.SimpleVirtualCameraActivity.EXTRA_VIDEO_PATH, videoToLoad)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                        
                                        val videoName = selectedVideo.takeIf { it != "Sample Video 1 (MP4)" } ?: "Selected Video"
                                        Toast.makeText(context, "🎬 YOUR SELECTED VIDEO NOW APPEARS IN ALL CAMERA SURFACES!\n🎯 TikTok, Instagram, etc. will show: $videoName\n📱 Open camera apps to test!", Toast.LENGTH_LONG).show()

                                        isProcessing = false
                                    }

                                    val status = surfaceInjection.getInjectionStatus()
                                    Log.d("HomeScreen", "Simple camera surface injection status:\n$status")
                                    
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "❌ Failed to setup camera surface video injection", Toast.LENGTH_SHORT).show()
                                        isProcessing = false
                                    }
                                }
                                
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isProcessing = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProcessing) Color(0xFF9E9E9E) else Color(0xFF4CAF50)
                    )
                ) {
                    if (isProcessing) {
                        Text(
                            text = "STARTING...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PLAY",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = { 
                        if (isProcessing) return@OutlinedButton
                        
                        isPlaying = false
                        isProcessing = true
                        

                        Toast.makeText(context, "🔄 Stopping camera surface video injection...", Toast.LENGTH_SHORT).show()
                        

                        GlobalScope.launch(Dispatchers.IO) {
                            try {

                                val surfaceInjection = SimpleCameraSurfaceInjection.instance
                                surfaceInjection.disableCameraSurfaceInjection()
                                

                                val systemHook = SystemWideCameraHook.instance
                                systemHook.disableCompleteSystemWideVirtualCamera()

                                withContext(Dispatchers.Main) {
                                    val serviceIntent = Intent(context, com.app001.virtualcamera.service.SystemWideVirtualCameraService::class.java).apply {
                                        action = com.app001.virtualcamera.service.SystemWideVirtualCameraService.ACTION_STOP_SYSTEM_CAMERA
                                    }
                                    context.startService(serviceIntent)
                                    
                                    Toast.makeText(context, "🔄 Camera Surface Video Injection Stopped!\n📱 Normal camera surfaces restored for all apps.", Toast.LENGTH_LONG).show()
                                    isProcessing = false
                                }
                                
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "❌ Error stopping: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isProcessing = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isProcessing) Color(0xFF9E9E9E) else Color(0xFFE65100)
                    )
                ) {
                    if (isProcessing) {
                        Text(
                            text = "STOPPING...",
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STOP",
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Floating Window Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "floating window:",
                    fontSize = 14.sp,
                    color = Color(0xFF333333)
                )
                
                Switch(
                    checked = true,
                    onCheckedChange = { },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFE91E63)
                    )
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(

) {
    val context = LocalContext.current
    var isPreviewActive by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "3. Preview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            
            // Preview Texture/Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        if (isPreviewActive) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPreviewActive) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera Preview",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF2196F3)
                        )
                        Text(
                            text = "Preview Active",
                            fontSize = 12.sp,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "No Preview",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF666666)
                        )
                        Text(
                            text = "No Preview",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        isPreviewActive = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        text = "PREVIEW",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                OutlinedButton(
                    onClick = { isPreviewActive = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE65100)
                    )
                ) {
                    Text(
                        text = "STOP",
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionInfo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "V2.0.40.54",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "all copyright reserved",
            fontSize = 12.sp,
            color = Color(0xFF999999)
        )
    }
}

@Composable
private fun FeaturesShowcase() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✨",
                    fontSize = 24.sp
                )
                Text(
                    text = "Amazing Features",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748)
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernFeatureItem(
                    icon = "🎥",
                    title = "Video Preview",
                    description = "Watch your videos with auto-play magic"
                )
                
                ModernFeatureItem(
                    icon = "🔄",
                    title = "Camera Replacement",
                    description = "Replace camera preview with any video"
                )
                
                ModernFeatureItem(
                    icon = "🎨",
                    title = "Custom Settings",
                    description = "Personalize your virtual camera experience"
                )
            }
        }
    }
}

@Composable
private fun ModernFeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.size(48.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF7FAFC)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp
                )
            }
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color(0xFF718096)
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
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

                Text(
                    text = "Root Access Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Preview replacement needs superuser permissions to replace camera preview with your video while keeping the system camera functional.",
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
                            text = "To enable Preview Replacement:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        
                        Text(
                            text = "1. Open your root manager app (SuperSU, Magisk, etc.)\n2. Grant superuser permissions to Preview Replacement\n3. Return to this app and tap 'Request Root Access'\n4. Preview replacement will be ready to use!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }

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
                        text = "Root access is required for preview replacement functionality",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                }
                

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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


private fun requestRootAccess(context: Context, systemVirtualCamera: SystemVirtualCamera) {
    try {

        val hasRoot = systemVirtualCamera.isDeviceRooted()
        
        if (hasRoot) {
            Toast.makeText(context, "✅ Root access granted! Preview replacement is ready to use.", Toast.LENGTH_LONG).show()
        } else {

            Toast.makeText(context, "🔐 Requesting root access...", Toast.LENGTH_SHORT).show()
            

            val rootResult = systemVirtualCamera.programmaticallyRequestRootAccess()
            
            if (rootResult.success) {
                Toast.makeText(context, "✅ ${rootResult.message}", Toast.LENGTH_LONG).show()
            } else {

                Toast.makeText(context, "🔄 Trying alternative root request methods...", Toast.LENGTH_SHORT).show()
                
                val forceResult = systemVirtualCamera.forceRootPermissionRequest()
                
            if (forceResult) {
                Toast.makeText(context, "✅ Root access granted via force request! Preview replacement ready.", Toast.LENGTH_LONG).show()
            } else {
                    showRootInstructionsDialog(context)
                }
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error requesting root access: ${e.message}", Toast.LENGTH_SHORT).show()
        showRootInstructionsDialog(context)
    }
}


private fun requestSuperuserPermissions(context: Context, systemVirtualCamera: SystemVirtualCamera): Boolean {
    return try {
        systemVirtualCamera.requestSuperuserPermissions()
    } catch (e: Exception) {
        false
    }
}


private fun showRootInstructionsDialog(context: Context) {
    val instructions = """
        🔐 Root Access Required
        
        Preview replacement needs superuser permissions to replace camera preview with your video.
        
        To enable Preview Replacement:
        
        1. Open your root manager app:
           • SuperSU
           • Magisk Manager
           • KingRoot
           • Or any other root manager
        
        2. Look for "Preview Replacement" or "Virtual Camera" in the app list
        
        3. Grant "Allow" or "Grant" permissions
        
        4. Return to Preview Replacement and try again
        
        Note: If you don't have a root manager, you need to root your device first.
    """.trimIndent()
    
    Toast.makeText(context, instructions, Toast.LENGTH_LONG).show()
}

private fun checkCameraAppStatus(
    systemVirtualCamera: SystemVirtualCamera,
    currentEnabled: List<String>,
    currentDisabled: List<String>,
    onResult: (List<String>, List<String>) -> Unit
) {
    try {
        val enabled = systemVirtualCamera.getEnabledCameraApps()
        val disabled = systemVirtualCamera.getDisabledCameraApps()
        onResult(enabled, disabled)
    } catch (e: Exception) {
        onResult(currentEnabled, currentDisabled)
    }
}

private fun checkCameraAppStatusAsync(
    systemVirtualCamera: SystemVirtualCamera,
    currentEnabled: List<String>,
    currentDisabled: List<String>,
    onResult: (List<String>, List<String>) -> Unit
) {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            val enabled = systemVirtualCamera.getEnabledCameraApps()
            val disabled = systemVirtualCamera.getDisabledCameraApps()
            
            withContext(Dispatchers.Main) {
                onResult(enabled, disabled)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(currentEnabled, currentDisabled)
            }
        }
    }
}

private fun restoreDefaultCamera(
    context: Context,
    systemVirtualCamera: SystemVirtualCamera,
    onResult: (List<String>, List<String>) -> Unit
) {
    Toast.makeText(context, "🔧 Restoring default camera apps...", Toast.LENGTH_SHORT).show()
    GlobalScope.launch(Dispatchers.IO) {
        try {
            Log.d("HomeScreen", "Starting camera restoration in background thread")
            val success = systemVirtualCamera.restoreDefaultCameraApps()

            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Default camera apps restored successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Some camera apps may already be enabled", Toast.LENGTH_LONG).show()
                }

                checkCameraAppStatusAsync(systemVirtualCamera, emptyList(), emptyList()) { enabled, disabled ->
                    onResult(enabled, disabled)
                }
            }
            
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error restoring default camera: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error restoring camera: ${e.message}", Toast.LENGTH_LONG).show()
                onResult(emptyList(), emptyList())
            }
        }
    }
}

private fun performComprehensiveRestore(
    context: Context,
    systemVirtualCamera: SystemVirtualCamera,
    onResult: (List<String>, List<String>) -> Unit
) {
    try {
        Toast.makeText(context, "🔧 Starting comprehensive camera restoration...", Toast.LENGTH_LONG).show()

        val progressDialog = android.app.ProgressDialog(context).apply {
            setTitle("Comprehensive Camera Restore")
            setMessage("Performing system-level camera restoration...")
            setCancelable(false)
            show()
        }

        Thread {
            try {
                val success = systemVirtualCamera.comprehensiveCameraRestore()

                (context as android.app.Activity).runOnUiThread {
                    progressDialog.dismiss()
                    
                    if (success) {
                        Toast.makeText(context, "Comprehensive camera restoration completed!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Partial restoration completed. Check logs for details.", Toast.LENGTH_LONG).show()
                    }

                    checkCameraAppStatusAsync(systemVirtualCamera, emptyList(), emptyList(), onResult)
                }
            } catch (e: Exception) {
                (context as android.app.Activity).runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(context, "Error in comprehensive restore: ${e.message}", Toast.LENGTH_LONG).show()
                    onResult(emptyList(), emptyList())
                }
            }
        }.start()
    } catch (e: Exception) {
        Toast.makeText(context, "Error starting comprehensive restore: ${e.message}", Toast.LENGTH_SHORT).show()
        onResult(emptyList(), emptyList())
    }
}


private fun performDebugRestore(
    context: Context,
    systemVirtualCamera: SystemVirtualCamera
) {
    try {
        Toast.makeText(context, "🔍 Running camera debug...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val debugInfo = systemVirtualCamera.debugCameraRestore()

                (context as android.app.Activity).runOnUiThread {
                    android.app.AlertDialog.Builder(context)
                        .setTitle("Camera Debug Info")
                        .setMessage(debugInfo)
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .setNeutralButton("Copy") { _, _ ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Debug Info", debugInfo)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Debug info copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .show()
                }
            } catch (e: Exception) {
                (context as android.app.Activity).runOnUiThread {
                    Toast.makeText(context, "Debug error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    } catch (e: Exception) {
        Toast.makeText(context, "Error starting debug: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun CameraRestoreDialog(
    enabledApps: List<String>,
    disabledApps: List<String>,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onRefresh: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
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

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Color(0xFF1976D2),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Camera",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Text(
                    text = "Restore Default Camera",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "This will re-enable any disabled system camera apps on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Current Status:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        
                        Text(
                            text = "Enabled: ${enabledApps.size} camera apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                        
                        Text(
                            text = "Disabled: ${disabledApps.size} camera apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                        
                        if (disabledApps.isNotEmpty()) {
                            Text(
                                text = "Disabled apps: ${disabledApps.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1976D2)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh")
                    }

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

                    Button(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore")
                    }
                }
            }
        }
    }
}

@Composable
private fun GhostCamCard(

) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {  },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                Text(
                    text = "🎥 GhostCam Style",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Navigate",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = "Select virtual camera as a separate device in any app",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📱",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "Selectable Device",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎯",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "All Apps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔧",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "Root Required",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Text(
                text = "Perfect for TikTok, Telegram, WhatsApp, Instagram and more!",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

private fun setupVirtualCameraReplacement(
    context: Context,
    systemVirtualCamera: SystemVirtualCamera,
    onResult: (Boolean, String) -> Unit
) {
    try {
        Toast.makeText(context, "🚀 Setting up virtual camera replacement...", Toast.LENGTH_SHORT).show()

        val progressDialog = android.app.ProgressDialog(context).apply {
            setTitle("Virtual Camera Setup")
            setMessage("Installing system-wide virtual camera...")
            setCancelable(false)
            show()
        }

        Thread {
            try {

                val installed = systemVirtualCamera.installSystemVirtualCamera()
                if (!installed) {
                    (context as android.app.Activity).runOnUiThread {
                        progressDialog.dismiss()
                        onResult(false, "Failed to install virtual camera system")
                    }
                    return@Thread
                }

                val defaultVideoPath = "/storage/emulated/0/Download/sample_video.mp4"
                val feedStarted = systemVirtualCamera.startVirtualCameraFeed(defaultVideoPath)
                if (!feedStarted) {

                    val assetVideoPath = "android.resource://${context.packageName}/raw/sample_video_tt"
                    val fallbackStarted = systemVirtualCamera.startVirtualCameraFeed(assetVideoPath)
                    
                    if (!fallbackStarted) {
                        (context as android.app.Activity).runOnUiThread {
                            progressDialog.dismiss()
                            onResult(false, "Failed to start virtual camera feed")
                        }
                        return@Thread
                    }
                }

                val videoPath = "/storage/emulated/0/Download/sample_video.mp4"
                val completeSetup = systemVirtualCamera.setupCompleteVirtualCameraHack(videoPath)
                

                (context as android.app.Activity).runOnUiThread {
                    progressDialog.dismiss()
                    
                    if (completeSetup) {
                        onResult(true, "✅ Virtual Camera Replacement Setup Complete!\n🎯 ALL camera apps will now show virtual video\n📱 Open TikTok, Instagram, or any camera app to test!")
                    } else {
                        onResult(false, "Virtual camera installed but complete setup failed")
                    }
                }
                
            } catch (e: Exception) {
                (context as android.app.Activity).runOnUiThread {
                    progressDialog.dismiss()
                    onResult(false, "Error: ${e.message}")
                }
            }
        }.start()
        
    } catch (e: Exception) {
        onResult(false, "Failed to start virtual camera setup: ${e.message}")
    }
}

