package com.app001.virtualcamera.service

import android.app.Service
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.util.Size
import com.app001.virtualcamera.system.SystemVirtualCamera

/**
 * Virtual Camera Provider Service
 * This service creates a virtual camera device that appears in the system camera list
 * Apps can then select this virtual camera instead of the real camera
 */
class VirtualCameraProviderService : Service() {
    
    companion object {
        private const val TAG = "VirtualCameraProvider"
        const val VIRTUAL_CAMERA_ID = "virtual_camera_ghostcam"
        const val VIRTUAL_CAMERA_NAME = "GhostCam Virtual Camera"
    }
    
    private val binder = VirtualCameraBinder()
    private var systemVirtualCamera: SystemVirtualCamera? = null
    private var isVirtualCameraRegistered = false
    
    inner class VirtualCameraBinder : Binder() {
        fun getService(): VirtualCameraProviderService = this@VirtualCameraProviderService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Virtual Camera Provider Service created")
        
        systemVirtualCamera = SystemVirtualCamera(this)
        registerVirtualCamera()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Virtual Camera Provider Service started")
        
        // Register virtual camera with system
        registerVirtualCamera()
        
        return START_STICKY // Restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    /**
     * Register virtual camera with the Android camera system
     * This makes it appear as a selectable camera device
     */
    private fun registerVirtualCamera() {
        try {
            Log.d(TAG, "Registering virtual camera with system...")
            
            // Set system properties to register virtual camera
            registerVirtualCameraProperties()
            
            // Install virtual camera hooks
            systemVirtualCamera?.installSystemVirtualCamera()
            
            // Set up camera characteristics for virtual camera
            setupVirtualCameraCharacteristics()
            
            isVirtualCameraRegistered = true
            
            Log.d(TAG, "✅ Virtual camera registered successfully!")
            Log.d(TAG, "📱 Apps can now select: $VIRTUAL_CAMERA_NAME")
            Log.d(TAG, "🎯 Camera ID: $VIRTUAL_CAMERA_ID")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register virtual camera: ${e.message}")
        }
    }
    
    /**
     * Set system properties to register virtual camera device
     */
    private fun registerVirtualCameraProperties() {
        try {
            // Register virtual camera in camera device list
            System.setProperty("camera.virtual.device.id", VIRTUAL_CAMERA_ID)
            System.setProperty("camera.virtual.device.name", VIRTUAL_CAMERA_NAME)
            System.setProperty("camera.virtual.device.enabled", "true")
            
            // Set virtual camera characteristics
            System.setProperty("camera.virtual.resolution.width", "640")
            System.setProperty("camera.virtual.resolution.height", "480")
            System.setProperty("camera.virtual.format", "NV21")
            System.setProperty("camera.virtual.fps", "30")
            
            // Enable virtual camera in camera manager
            System.setProperty("camera.virtual.manager.enabled", "true")
            System.setProperty("camera.virtual.provider.active", "true")
            
            Log.d(TAG, "Virtual camera properties set successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set virtual camera properties: ${e.message}")
        }
    }
    
    /**
     * Set up virtual camera characteristics that apps will see
     */
    private fun setupVirtualCameraCharacteristics() {
        try {
            // Set up camera characteristics for virtual camera
            val characteristics = createVirtualCameraCharacteristics()
            
            // Register characteristics with system
            System.setProperty("camera.virtual.characteristics", characteristics.toString())
            
            Log.d(TAG, "Virtual camera characteristics set up")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup virtual camera characteristics: ${e.message}")
        }
    }
    
    /**
     * Create camera characteristics for virtual camera
     */
    private fun createVirtualCameraCharacteristics(): Map<String, Any> {
        return mapOf(
            "CAMERA_ID" to VIRTUAL_CAMERA_ID,
            "CAMERA_NAME" to VIRTUAL_CAMERA_NAME,
            "LENS_FACING" to CameraCharacteristics.LENS_FACING_FRONT,
            "SENSOR_ORIENTATION" to 90,
            "SUPPORTED_PREVIEW_SIZES" to listOf(
                Size(640, 480),
                Size(320, 240),
                Size(1280, 720)
            ),
            "SUPPORTED_PICTURE_SIZES" to listOf(
                Size(640, 480),
                Size(1280, 720),
                Size(1920, 1080)
            ),
            "SUPPORTED_FORMATS" to listOf("NV21", "YV12", "RGB_565"),
            "MAX_PREVIEW_WIDTH" to 1280,
            "MAX_PREVIEW_HEIGHT" to 720,
            "MAX_PICTURE_WIDTH" to 1920,
            "MAX_PICTURE_HEIGHT" to 1080,
            "SUPPORTED_FPS_RANGES" to listOf(
                intArrayOf(15, 30),
                intArrayOf(30, 30)
            )
        )
    }
    
    /**
     * Start virtual camera feed
     */
    fun startVirtualCameraFeed(videoPath: String): Boolean {
        return try {
            Log.d(TAG, "Starting virtual camera feed with video: $videoPath")
            
            val success = systemVirtualCamera?.startVirtualCameraFeed(videoPath) ?: false
            
            if (success) {
                Log.d(TAG, "✅ Virtual camera feed started successfully!")
                Log.d(TAG, "📹 Video feed active: $videoPath")
            } else {
                Log.e(TAG, "❌ Failed to start virtual camera feed")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting virtual camera feed: ${e.message}")
            false
        }
    }
    
    /**
     * Stop virtual camera feed
     */
    fun stopVirtualCameraFeed() {
        try {
            Log.d(TAG, "Stopping virtual camera feed...")
            
            systemVirtualCamera?.stopVirtualCameraFeed()
            
            Log.d(TAG, "✅ Virtual camera feed stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping virtual camera feed: ${e.message}")
        }
    }
    
    /**
     * Check if virtual camera is active
     */
    fun isVirtualCameraActive(): Boolean {
        return systemVirtualCamera?.isVirtualCameraHackActivePublic() ?: false
    }
    
    /**
     * Unregister virtual camera from system
     */
    fun unregisterVirtualCamera() {
        try {
            Log.d(TAG, "Unregistering virtual camera from system...")
            
            // Stop virtual camera
            stopVirtualCameraFeed()
            
            // Uninstall system hooks
            systemVirtualCamera?.uninstallSystemVirtualCamera()
            
            // Clear system properties
            clearVirtualCameraProperties()
            
            isVirtualCameraRegistered = false
            
            Log.d(TAG, "✅ Virtual camera unregistered successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception unregistering virtual camera: ${e.message}")
        }
    }
    
    /**
     * Clear virtual camera system properties
     */
    private fun clearVirtualCameraProperties() {
        try {
            System.clearProperty("camera.virtual.device.id")
            System.clearProperty("camera.virtual.device.name")
            System.clearProperty("camera.virtual.device.enabled")
            System.clearProperty("camera.virtual.resolution.width")
            System.clearProperty("camera.virtual.resolution.height")
            System.clearProperty("camera.virtual.format")
            System.clearProperty("camera.virtual.fps")
            System.clearProperty("camera.virtual.manager.enabled")
            System.clearProperty("camera.virtual.provider.active")
            System.clearProperty("camera.virtual.characteristics")
            
            Log.d(TAG, "Virtual camera properties cleared")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear virtual camera properties: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Virtual Camera Provider Service destroyed")
        
        // Unregister virtual camera
        unregisterVirtualCamera()
    }
}
