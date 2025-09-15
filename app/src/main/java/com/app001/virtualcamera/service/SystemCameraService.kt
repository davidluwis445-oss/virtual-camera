package com.app001.virtualcamera.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.app001.virtualcamera.camera.SystemCameraProvider
import com.app001.virtualcamera.camera.VirtualCameraActivity
import com.app001.virtualcamera.utils.VideoPathManager

/**
 * System Camera Service that provides system-wide camera functionality
 * This service runs continuously and handles camera requests from all apps
 */
class SystemCameraService : Service() {
    companion object {
        private const val TAG = "SystemCameraService"
        const val ACTION_START_SYSTEM_CAMERA = "com.app001.virtualcamera.START_SYSTEM_CAMERA"
        const val ACTION_STOP_SYSTEM_CAMERA = "com.app001.virtualcamera.STOP_SYSTEM_CAMERA"
    }
    
    private lateinit var systemCameraProvider: SystemCameraProvider
    private var isRunning = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "System Camera Service created")
        
        systemCameraProvider = SystemCameraProvider(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SYSTEM_CAMERA -> {
                startSystemCamera()
            }
            ACTION_STOP_SYSTEM_CAMERA -> {
                stopSystemCamera()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun startSystemCamera() {
        try {
            Log.d(TAG, "Starting system camera service")
            
            // Initialize VideoPathManager
            VideoPathManager.initialize(this)
            
            // Register virtual camera with system
            val success = systemCameraProvider.registerVirtualCamera()
            
            if (success) {
                isRunning = true
                Log.d(TAG, "System camera service started successfully")
            } else {
                Log.e(TAG, "Failed to start system camera service")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting system camera service: ${e.message}")
        }
    }
    
    private fun stopSystemCamera() {
        try {
            Log.d(TAG, "Stopping system camera service")
            isRunning = false
            Log.d(TAG, "System camera service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping system camera service: ${e.message}")
        }
    }
    
    /**
     * Handle camera intent from external apps
     */
    fun handleCameraIntent(intent: Intent): Boolean {
        return try {
            Log.d(TAG, "Handling camera intent: ${intent.action}")
            
            // Get video path
            val videoPath = VideoPathManager.getCurrentVideoPath()
            
            if (videoPath == null) {
                Log.w(TAG, "No video path available, using test pattern")
            }
            
            // Create virtual camera intent
            val virtualCameraIntent = Intent(this, VirtualCameraActivity::class.java).apply {
                // Copy original intent action
                action = intent.action
                
                // Add video path
                putExtra(VirtualCameraActivity.EXTRA_VIDEO_PATH, videoPath)
                putExtra(VirtualCameraActivity.EXTRA_IS_VIRTUAL_CAMERA, true)
                
                // Copy data and extras
                if (intent.data != null) {
                    data = intent.data
                }
                intent.extras?.let { extras ->
                    putExtras(extras)
                }
                
                // Set flags
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            // Launch virtual camera
            startActivity(virtualCameraIntent)
            Log.d(TAG, "Virtual camera launched successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling camera intent: ${e.message}")
            false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "System Camera Service destroyed")
        isRunning = false
    }
}
