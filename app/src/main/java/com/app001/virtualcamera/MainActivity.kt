package com.app001.virtualcamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import com.app001.virtualcamera.ui.theme.VirtualCameraTheme
import com.app001.virtualcamera.ui.components.CameraPreviewScreen
import com.app001.virtualcamera.ui.viewmodel.VirtualCameraViewModel
import com.app001.virtualcamera.service.VirtualCameraService
import com.app001.virtualcamera.camera.VirtualCameraProvider

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: VirtualCameraViewModel
    private lateinit var virtualCameraProvider: VirtualCameraProvider

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.onPermissionsGranted()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = VirtualCameraViewModel()
        virtualCameraProvider = VirtualCameraProvider(this)
        virtualCameraProvider.initializeVirtualCamera()

        setContent {
            VirtualCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VirtualCameraApp(
                        viewModel = viewModel,
                        onRequestPermissions = { requestPermissions() }
                    )
                }
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.onPermissionsGranted()
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        requestPermissionLauncher.launch(permissions)
    }

    fun startVirtualCamera() {
        try {
            // Load sample video
            val videoLoaded = virtualCameraProvider.loadVideo("sample_video.mp4")
            if (!videoLoaded) {
                Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Start virtual camera
            virtualCameraProvider.startVirtualCamera()
            
            viewModel.startCamera()
            Toast.makeText(this, "Virtual camera started - test with camera apps", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start virtual camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVirtualCamera() {
        try {
            // Stop virtual camera
            virtualCameraProvider.stopVirtualCamera()
            
            viewModel.stopCamera()
            Toast.makeText(this, "Virtual camera stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to stop virtual camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualCameraProvider.release()
    }
}

@Composable
fun VirtualCameraApp(
    viewModel: VirtualCameraViewModel,
    onRequestPermissions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    when {
        !uiState.hasPermissions -> {
            PermissionScreen(onRequestPermissions = onRequestPermissions)
        }
        uiState.isLoading -> {
            LoadingScreen()
        }
        else -> {
            CameraPreviewScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Virtual Camera",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "This app needs camera and microphone permissions to work properly.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permissions")
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading Virtual Camera...")
        }
    }
}