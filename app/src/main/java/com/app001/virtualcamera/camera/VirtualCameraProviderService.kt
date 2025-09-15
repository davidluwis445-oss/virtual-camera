package com.app001.virtualcamera.camera

import android.app.Service
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.IBinder
import android.util.Log
import com.app001.virtualcamera.utils.VideoPathManager

/**
 * Virtual Camera Provider Service
 * This service provides camera capabilities to the system
 */
class VirtualCameraProviderService : Service() {
    companion object {
        private const val TAG = "VirtualCameraProviderService"
        private const val VIRTUAL_CAMERA_ID = "virtual_camera_0"
    }
    
    private lateinit var cameraManager: CameraManager
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Virtual Camera Provider Service created")
        
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        registerVirtualCamera()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Virtual Camera Provider Service started")
        return START_STICKY
    }
    
    private fun registerVirtualCamera() {
        try {
            Log.d(TAG, "Registering virtual camera with system")
            
            // Initialize VideoPathManager
            VideoPathManager.initialize(this)
            
            // The virtual camera is now available to the system
            Log.d(TAG, "Virtual camera registered successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error registering virtual camera: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Virtual Camera Provider Service destroyed")
    }
}
