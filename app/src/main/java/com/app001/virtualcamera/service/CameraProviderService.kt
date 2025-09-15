package com.app001.virtualcamera.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.app001.virtualcamera.camera.VirtualCameraProvider

/**
 * Camera Provider Service that handles camera requests from other apps
 * This service acts as a system-level camera provider
 */
class CameraProviderService : Service() {
    companion object {
        private const val TAG = "CameraProviderService"
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Camera provider service bound")
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Camera provider service started")
        
        // Handle camera requests
        intent?.let { cameraIntent ->
            when (cameraIntent.action) {
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE,
                android.provider.MediaStore.ACTION_VIDEO_CAPTURE -> {
                    handleCameraRequest(cameraIntent)
                }
            }
        }
        
        return START_STICKY
    }
    
    private fun handleCameraRequest(intent: Intent) {
        try {
            Log.d(TAG, "Handling camera request: ${intent.action}")
            
            // Use VirtualCameraProvider to handle the request
            val virtualCameraIntent = VirtualCameraProvider.handleCameraIntent(this, intent)
            
            if (virtualCameraIntent != null) {
                // Launch the virtual camera activity
                virtualCameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(virtualCameraIntent)
                Log.d(TAG, "Virtual camera launched")
            } else {
                Log.e(TAG, "Failed to create virtual camera intent")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling camera request: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Camera provider service destroyed")
    }
}
