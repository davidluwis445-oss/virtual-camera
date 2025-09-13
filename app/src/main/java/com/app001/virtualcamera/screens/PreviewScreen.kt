package com.app001.virtualcamera.screens

import android.Manifest
import android.content.ContentResolver
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun PreviewScreen(
    videoPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    selectedVideoUri: Uri?
) {
    val context = LocalContext.current
    var selectedVideoPath by remember { mutableStateOf("sample_video.mp4") }
    var selectedVideoName by remember { mutableStateOf("Sample Video") }
    var isVideoSelected by remember { mutableStateOf(false) }
    var showVideoPreview by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    // Handle video selection when URI changes
    LaunchedEffect(selectedVideoUri) {
        selectedVideoUri?.let { uri ->
            copyVideoToInternalStorage(
                context = context as ComponentActivity,
                videoUri = uri,
                onVideoSelected = { videoPath, videoName ->
                    selectedVideoPath = videoPath
                    selectedVideoName = videoName
                    isVideoSelected = true
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Video Preview",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        // Video Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isVideoSelected) Color(0xFFE8F5E8) else Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Video Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isVideoSelected) Color(0xFF2E7D32) else Color(0xFF666666)
                )
                
                if (isVideoSelected) {
                    Text(
                        text = "Selected: $selectedVideoName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32)
                    )
                } else {
                    Text(
                        text = "No video selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
                
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
                        imageVector = Icons.Default.VideoLibrary,
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
            }
        }

        // Preview Button
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

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How to Use",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                
                Text(
                    text = "1. Select a video file from your device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1976D2)
                )
                
                Text(
                    text = "2. Tap 'PREVIEW VIDEO' to see it in action",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1976D2)
                )
                
                Text(
                    text = "3. Use the preview to test before system integration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1976D2)
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
                releaseMediaPlayer(mediaPlayer) { mp -> mediaPlayer = mp }
                isVideoPlaying = false
                showVideoPreview = false 
            },
            onPlayPause = {
                if (isVideoPlaying) {
                    stopVideoPlayback(mediaPlayer) { isPlaying -> isVideoPlaying = isPlaying }
                } else {
                    startVideoPlayback(
                        context = context as ComponentActivity,
                        videoPath = selectedVideoPath,
                        textureView = textureView,
                        onVideoPlaying = { isPlaying -> isVideoPlaying = isPlaying },
                        onMediaPlayer = { mp -> mediaPlayer = mp }
                    )
                }
            },
            onTextureViewReady = { tv ->
                textureView = tv
            }
        )
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
        // Auto-play will be handled by the parent
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
                            Log.d("PreviewScreen", "SurfaceTexture available: ${width}x${height}")
                            textureView = this@apply
                        }
                        
                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                            Log.d("PreviewScreen", "SurfaceTexture size changed: ${width}x${height}")
                        }
                        
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            Log.d("PreviewScreen", "SurfaceTexture destroyed")
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

// Helper functions for video management
private fun copyVideoToInternalStorage(
    context: ComponentActivity,
    videoUri: Uri,
    onVideoSelected: (String, String) -> Unit
) {
    try {
        val contentResolver: ContentResolver = context.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(videoUri)
        
        if (inputStream != null) {
            // Get video name from URI
            val videoName = getVideoNameFromUri(context, videoUri)
            
            // Validate video format
            if (!isValidVideoFormat(videoName)) {
                Toast.makeText(context, "Unsupported video format. Please select MP4, AVI, or MOV file.", Toast.LENGTH_LONG).show()
                inputStream.close()
                return
            }
            
            // Create internal storage file
            val internalFile = File(context.filesDir, "selected_video.mp4")
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
            onVideoSelected(internalFile.absolutePath, videoName)
            
            Toast.makeText(context, "Video selected: $videoName", Toast.LENGTH_LONG).show()
            Log.d("PreviewScreen", "Video copied to: ${internalFile.absolutePath}")
        } else {
            Toast.makeText(context, "Failed to open selected video", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("PreviewScreen", "Error copying video: ${e.message}")
        Toast.makeText(context, "Error copying video: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun isValidVideoFormat(fileName: String): Boolean {
    val supportedFormats = listOf(".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv", ".webm")
    val lowerCaseName = fileName.lowercase()
    return supportedFormats.any { lowerCaseName.endsWith(it) }
}

private fun getVideoNameFromUri(context: ComponentActivity, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
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

private fun startVideoPlayback(
    context: ComponentActivity,
    videoPath: String,
    textureView: TextureView?,
    onVideoPlaying: (Boolean) -> Unit,
    onMediaPlayer: (MediaPlayer?) -> Unit
) {
    try {
        textureView?.let { tv ->
            val surfaceTexture = tv.surfaceTexture
            if (surfaceTexture != null) {
                val surface = Surface(surfaceTexture)
                
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(videoPath)
                    setSurface(surface)
                    setOnPreparedListener { mp ->
                        mp.start()
                        onVideoPlaying(true)
                        Log.d("PreviewScreen", "Video playback started with SurfaceTexture")
                    }
                    setOnCompletionListener {
                        onVideoPlaying(false)
                        Log.d("PreviewScreen", "Video playback completed")
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("PreviewScreen", "MediaPlayer error: what=$what, extra=$extra")
                        onVideoPlaying(false)
                        Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
                        true
                    }
                    prepareAsync()
                }
                onMediaPlayer(mediaPlayer)
            } else {
                Log.w("PreviewScreen", "SurfaceTexture not available yet")
                Toast.makeText(context, "Video surface not ready, please try again", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.w("PreviewScreen", "TextureView not available")
            Toast.makeText(context, "Video player not ready, please try again", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("PreviewScreen", "Error starting video playback: ${e.message}")
        Toast.makeText(context, "Error starting video playback: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun stopVideoPlayback(
    mediaPlayer: MediaPlayer?,
    onVideoPlaying: (Boolean) -> Unit
) {
    try {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            onVideoPlaying(false)
            Log.d("PreviewScreen", "Video playback stopped")
        }
    } catch (e: Exception) {
        Log.e("PreviewScreen", "Error stopping video playback: ${e.message}")
    }
}

private fun releaseMediaPlayer(
    mediaPlayer: MediaPlayer?,
    onMediaPlayer: (MediaPlayer?) -> Unit
) {
    try {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
            onMediaPlayer(null)
            Log.d("PreviewScreen", "MediaPlayer released")
        }
    } catch (e: Exception) {
        Log.e("PreviewScreen", "Error releasing MediaPlayer: ${e.message}")
    }
}
