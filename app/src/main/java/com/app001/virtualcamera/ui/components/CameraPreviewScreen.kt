package com.app001.virtualcamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.app001.virtualcamera.ui.viewmodel.VirtualCameraViewModel
import com.app001.virtualcamera.camera.VirtualCameraPreview

@Composable
fun CameraPreviewScreen(
    viewModel: VirtualCameraViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = modifier) {
        // Camera Preview
        VirtualCameraPreviewView(
            modifier = Modifier.fillMaxSize(),
            isActive = uiState.isCameraActive,
            videoPath = uiState.currentVideoPath,
            isVideoPlaying = uiState.isVideoPlaying
        )

        // Top Controls
        TopControls(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            isCameraActive = uiState.isCameraActive,
            isVideoPlaying = uiState.isVideoPlaying,
            onStartCamera = { 
                // Start virtual camera for all apps
                viewModel.startCamera()
            },
            onStopCamera = { 
                // Stop virtual camera
                viewModel.stopCamera()
            },
            onStartVideo = { videoPath -> viewModel.startVideo(videoPath) },
            onStopVideo = { viewModel.stopVideo() }
        )

        // Bottom Controls
        BottomControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            isCameraActive = uiState.isCameraActive,
            isVideoPlaying = uiState.isVideoPlaying
        )

        // Error Message
        uiState.errorMessage?.let { error ->
            ErrorMessage(
                message = error,
                onDismiss = { /* Clear error */ },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun VirtualCameraPreviewView(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    videoPath: String?,
    isVideoPlaying: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as? LifecycleOwner

    AndroidView(
        factory = { ctx ->
            VirtualCameraPreview(ctx).apply {
                if (isActive) {
                    startPreview()
                    if (isVideoPlaying && videoPath != null) {
                        loadVideo(videoPath)
                    }
                }
            }
        },
        update = { preview ->
            if (isActive) {
                preview.startPreview()
                if (isVideoPlaying && videoPath != null) {
                    preview.loadVideo(videoPath)
                }
            } else {
                preview.stopPreview()
            }
        },
        modifier = modifier
    )
}

@Composable
fun TopControls(
    modifier: Modifier = Modifier,
    isCameraActive: Boolean,
    isVideoPlaying: Boolean,
    onStartCamera: () -> Unit,
    onStopCamera: () -> Unit,
    onStartVideo: (String) -> Unit,
    onStopVideo: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Camera Control
        IconButton(
            onClick = if (isCameraActive) onStopCamera else onStartCamera,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isCameraActive) Color.Red else Color.White
            )
        ) {
            Icon(
                imageVector = if (isCameraActive) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isCameraActive) "Stop Camera" else "Start Camera"
            )
        }

        // Status Text
        Text(
            text = when {
                isVideoPlaying -> "Video Playing"
                isCameraActive -> "Camera Active"
                else -> "Camera Inactive"
            },
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        // Video Control
        IconButton(
            onClick = { if (isVideoPlaying) onStopVideo() else onStartVideo("sample_video.mp4") },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isVideoPlaying) Color.Red else Color.White
            )
        ) {
            Icon(
                imageVector = if (isVideoPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isVideoPlaying) "Stop Video" else "Play Video"
            )
        }
    }
}

@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    isCameraActive: Boolean,
    isVideoPlaying: Boolean
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings Button
        IconButton(
            onClick = { /* Open settings */ },
            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }

        // Record Button (if camera is active)
        if (isCameraActive) {
            IconButton(
                onClick = { /* Start/Stop recording */ },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Record"
                )
            }
        }

        // Video Library Button
        IconButton(
            onClick = { /* Open video library */ },
            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Video Library"
            )
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
