package com.app001.virtualcamera

import android.Manifest
import android.content.ContentResolver
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import com.app001.virtualcamera.system.SystemVirtualCamera
import com.app001.virtualcamera.ui.theme.VirtualCameraTheme

class GhostCamActivity : ComponentActivity() {
    private lateinit var systemVirtualCamera: SystemVirtualCamera
    private var isVirtualCameraActive by mutableStateOf(false)
    private var isDeviceRooted by mutableStateOf(false)
    private var selectedVideoPath by mutableStateOf("sample_video.mp4")
    private var selectedVideoName by mutableStateOf("Sample Video")
    private var isVideoSelected by mutableStateOf(false)
    private var showVideoPreview by mutableStateOf(false)
    var isVideoPlaying by mutableStateOf(false)
        private set
    private var mediaPlayer: MediaPlayer? = null
    var textureView: TextureView? = null
        private set

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedVideoUri ->
            copyVideoToInternalStorage(selectedVideoUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        systemVirtualCamera = SystemVirtualCamera(this)
        isDeviceRooted = systemVirtualCamera.isDeviceRooted()

        setContent {
            VirtualCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GhostCamApp()
                }
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        requestPermissionLauncher.launch(permissions)
    }

    private fun copyVideoToInternalStorage(videoUri: Uri) {
        try {
            val contentResolver: ContentResolver = contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(videoUri)
            
            if (inputStream != null) {
                // Get video name from URI
                val videoName = getVideoNameFromUri(videoUri)
                
                // Validate video format
                if (!isValidVideoFormat(videoName)) {
                    Toast.makeText(this, "Unsupported video format. Please select MP4, AVI, or MOV file.", Toast.LENGTH_LONG).show()
                    inputStream.close()
                    return
                }
                
                // Create internal storage file
                val internalFile = File(filesDir, "selected_video.mp4")
                val outputStream = FileOutputStream(internalFile)
                
                // Copy video data
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                
                inputStream.close()
                outputStream.close()
                
                // Update state
                selectedVideoPath = internalFile.absolutePath
                selectedVideoName = videoName
                isVideoSelected = true
                
                Toast.makeText(this, "Video selected: $videoName", Toast.LENGTH_LONG).show()
                Log.d("GhostCam", "Video copied to: ${internalFile.absolutePath}")
            } else {
                Toast.makeText(this, "Failed to open selected video", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("GhostCam", "Error copying video: ${e.message}")
            Toast.makeText(this, "Error copying video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidVideoFormat(fileName: String): Boolean {
        val supportedFormats = listOf(".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv", ".webm")
        val lowerCaseName = fileName.lowercase()
        return supportedFormats.any { lowerCaseName.endsWith(it) }
    }

    private fun getVideoNameFromUri(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    it.getString(nameIndex) ?: "Unknown Video"
                } else {
                    "Unknown Video"
                }
            } else {
                "Unknown Video"
            }
        } ?: "Unknown Video"
    }

    fun installSystemVirtualCamera() {
        if (!isDeviceRooted) {
            Toast.makeText(this, "Device must be rooted to install system virtual camera", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val success = systemVirtualCamera.installSystemVirtualCamera()
            if (success) {
                Toast.makeText(this, "System virtual camera installed successfully", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to install system virtual camera", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error installing system virtual camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVirtualCameraFeed() {
        if (!isDeviceRooted) {
            Toast.makeText(this, "Device must be rooted to start virtual camera feed", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val success = systemVirtualCamera.startVirtualCameraFeed(selectedVideoPath)
            if (success) {
                isVirtualCameraActive = true
                val videoName = if (isVideoSelected) selectedVideoName else "Sample Video"
                Toast.makeText(this, "Virtual camera feed started with: $videoName", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to start virtual camera feed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting virtual camera feed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVirtualCameraFeed() {
        if (!isDeviceRooted) {
            Toast.makeText(this, "Device must be rooted to stop virtual camera feed", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val success = systemVirtualCamera.stopVirtualCameraFeed()
            if (success) {
                isVirtualCameraActive = false
                Toast.makeText(this, "Virtual camera feed stopped", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to stop virtual camera feed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping virtual camera feed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVideoPlayback(videoPath: String) {
        try {
            releaseMediaPlayer()
            
            textureView?.let { tv ->
                val surfaceTexture = tv.surfaceTexture
                if (surfaceTexture != null) {
                    val surface = Surface(surfaceTexture)
                    
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(videoPath)
                        setSurface(surface)
                        setOnPreparedListener { mp ->
                            mp.start()
                            isVideoPlaying = true
                            Log.d("GhostCam", "Video playback started with SurfaceTexture")
                        }
                        setOnCompletionListener {
                            isVideoPlaying = false
                            Log.d("GhostCam", "Video playback completed")
                        }
                        setOnErrorListener { _, what, extra ->
                            Log.e("GhostCam", "MediaPlayer error: what=$what, extra=$extra")
                            isVideoPlaying = false
                            Toast.makeText(this@GhostCamActivity, "Error playing video", Toast.LENGTH_SHORT).show()
                            true
                        }
                        prepareAsync()
                    }
                } else {
                    Log.w("GhostCam", "SurfaceTexture not available yet")
                    Toast.makeText(this, "Video surface not ready, please try again", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Log.w("GhostCam", "TextureView not available")
                Toast.makeText(this, "Video player not ready, please try again", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("GhostCam", "Error starting video playback: ${e.message}")
            Toast.makeText(this, "Error starting video playback: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVideoPlayback() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                isVideoPlaying = false
                Log.d("GhostCam", "Video playback stopped")
            }
        } catch (e: Exception) {
            Log.e("GhostCam", "Error stopping video playback: ${e.message}")
        }
    }

    fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
                mediaPlayer = null
                isVideoPlaying = false
                Log.d("GhostCam", "MediaPlayer released")
            }
        } catch (e: Exception) {
            Log.e("GhostCam", "Error releasing MediaPlayer: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    @Composable
    fun GhostCamApp() {
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
                // Camera Icon (simplified version of the aperture icon)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Color(0xFF4CAF50),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📷",
                        fontSize = 30.sp,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "Virtual Replace Camera",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Video Info Display
            if (isVideoSelected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Selected Video:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedVideoName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Main Action Buttons (Full-width, Solid Green)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SELECT VIDEO
                Button(
                    onClick = { 
                        videoPickerLauncher.launch("video/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Select Video",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVideoSelected) "CHANGE VIDEO" else "SELECT VIDEO",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // PREVIEW VIDEO
                if (isVideoSelected) {
                    Button(
                        onClick = { 
                            showVideoPreview = true
                        },
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
                            text = "PREVIEW VIDEO",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // SELECT APP
                Button(
                    onClick = { 
                        // Open app selection dialog
                        Toast.makeText(this@GhostCamActivity, "Select App", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        text = "SELECT APP",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // CREATE GHOSTCAM (VIDEO)
                Button(
                    onClick = { 
                        installSystemVirtualCamera()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    enabled = isDeviceRooted
                ) {
                    Text(
                        text = "CREATE GHOSTCAM (VIDEO)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // CREATE GHOSTCAM (REAL-TIME)
                Button(
                    onClick = { 
                        startVirtualCameraFeed()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    enabled = isDeviceRooted
                ) {
                    Text(
                        text = "CREATE GHOSTCAM (REAL-TIME)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ON/OFF GHOSTCAM
                Button(
                    onClick = { 
                        if (isVirtualCameraActive) {
                            stopVirtualCameraFeed()
                        } else {
                            startVirtualCameraFeed()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVirtualCameraActive) Color(0xFFF44336) else Color(0xFF4CAF50)
                    ),
                    enabled = isDeviceRooted
                ) {
                    Text(
                        text = if (isVirtualCameraActive) "OFF GHOSTCAM" else "ON GHOSTCAM",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle/Utility Buttons (Two-column, Green Outline)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ON/OFF WARNING
                    OutlinedButton(
                        onClick = { 
                            Toast.makeText(this@GhostCamActivity, "Toggle Warning", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        ),
                        border = BorderStroke(2.dp, Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = "ON/OFF WARNING",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ON/OFF SOUND
                    OutlinedButton(
                        onClick = { 
                            Toast.makeText(this@GhostCamActivity, "Toggle Sound", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        ),
                        border = BorderStroke(2.dp, Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = "ON/OFF SOUND",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ON/OFF EDITOR
                    OutlinedButton(
                        onClick = { 
                            Toast.makeText(this@GhostCamActivity, "Toggle Editor", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        ),
                        border = BorderStroke(2.dp, Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = "ON/OFF EDITOR",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // RANDOM IP-MAC
                    OutlinedButton(
                        onClick = { 
                            Toast.makeText(this@GhostCamActivity, "Random IP-MAC", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        ),
                        border = BorderStroke(2.dp, Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = "RANDOM IP-MAC",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SUPPORT
                Button(
                    onClick = { 
                        Toast.makeText(this@GhostCamActivity, "Support", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        text = "SUPPORT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // LOGOUT
                Button(
                    onClick = { 
                        finish()
                    },
                    modifier = Modifier.width(120.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        text = "LOGOUT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Status Indicator (Hidden by default, shown when needed)
            if (!isDeviceRooted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Text(
                        text = "⚠️ Device must be rooted to use GhostCam",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFD32F2F),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Video Preview Dialog
        if (showVideoPreview) {
            VideoPreviewDialog(
                videoName = selectedVideoName,
                videoPath = selectedVideoPath,
                isVideoPlaying = isVideoPlaying,
                onDismiss = { 
                    releaseMediaPlayer()
                    showVideoPreview = false 
                },
                onPlayPause = {
                    if (isVideoPlaying) {
                        stopVideoPlayback()
                    } else {
                        startVideoPlayback(selectedVideoPath)
                    }
                },
                onTextureViewReady = { tv ->
                    textureView = tv
                }
            )
        }
    }
}

@Composable
fun VideoPreviewDialog(
    videoName: String,
    videoPath: String,
    isVideoPlaying: Boolean,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onTextureViewReady: (TextureView) -> Unit
) {
    val context = LocalContext.current
    
    // Auto-start video playback when dialog opens
    LaunchedEffect(Unit) {
        // Small delay to ensure TextureView is ready
        kotlinx.coroutines.delay(500)
        (context as GhostCamActivity).startVideoPlayback(videoPath)
    }
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
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Preview",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Video Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2E2E2E)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Video Name:",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFB0B0B0)
                        )
                        Text(
                            text = videoName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Video Path:",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFB0B0B0)
                        )
                        Text(
                            text = videoPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCCCCCC),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Video Player with SurfaceTexture
                VideoPlayerView(
                    videoPath = videoPath,
                    isPlaying = isVideoPlaying,
                    onPlayPause = onPlayPause,
                    onTextureViewReady = onTextureViewReady
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onPlayPause,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVideoPlaying) Color(0xFFF44336) else Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            imageVector = if (isVideoPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isVideoPlaying) "Stop" else "Play",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isVideoPlaying) "Stop" else "Play")
                    }
                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF666666))
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerView(
    videoPath: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onTextureViewReady: (TextureView) -> Unit
) {
    val context = LocalContext.current
    var textureView by remember { mutableStateOf<TextureView?>(null) }
    
    // Update the textureView reference via callback
    LaunchedEffect(textureView) {
        textureView?.let { onTextureViewReady(it) }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF000000))
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            Log.d("GhostCam", "SurfaceTexture available: ${width}x${height}")
                            textureView = this@apply
                        }
                        
                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                            Log.d("GhostCam", "SurfaceTexture size changed: ${width}x${height}")
                        }
                        
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            Log.d("GhostCam", "SurfaceTexture destroyed")
                            textureView = null
                            return true
                        }
                        
                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                            // Frame updated
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Play/Pause overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Color(0x80000000),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }
        
        // Video info overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = if (isPlaying) "Playing..." else "Tap to play",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .background(
                        Color(0x80000000),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

