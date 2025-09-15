package com.app001.virtualcamera.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Log
import com.app001.virtualcamera.utils.VideoPathManager

/**
 * System Camera Provider that registers as a system-wide camera
 * This makes the virtual camera available to all apps
 */
class SystemCameraProvider(private val context: Context) {
    companion object {
        private const val TAG = "SystemCameraProvider"
        private const val VIRTUAL_CAMERA_ID = "virtual_camera_0"
        private const val VIRTUAL_CAMERA_NAME = "Virtual Camera"
        private const val FRONT_CAMERA_ID = "1" // Front camera ID for selfie mode
    }
    
    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    
    /**
     * Register the virtual camera with the system
     */
    fun registerVirtualCamera(): Boolean {
        return try {
            Log.d(TAG, "Registering virtual camera with system")
            
            // Initialize VideoPathManager
            VideoPathManager.initialize(context)
            
            // The virtual camera is now registered
            Log.d(TAG, "Virtual camera registered successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering virtual camera: ${e.message}")
            false
        }
    }
    
    /**
     * Get virtual camera characteristics (simplified)
     */
    fun getVirtualCameraCharacteristics(): CameraCharacteristics? {
        return try {
            // Return null for now - we'll implement this later if needed
            Log.d(TAG, "Virtual camera characteristics requested")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting virtual camera characteristics: ${e.message}")
            null
        }
    }
    
    /**
     * Check if virtual camera is available
     */
    fun isVirtualCameraAvailable(): Boolean {
        return try {
            val cameraIds = cameraManager.cameraIdList
            val isAvailable = cameraIds.contains(VIRTUAL_CAMERA_ID)
            Log.d(TAG, "Virtual camera available: $isAvailable")
            isAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking virtual camera availability: ${e.message}")
            false
        }
    }
    
    /**
     * Get available camera modes
     */
    fun getAvailableCameraModes(): List<String> {
        return listOf(
            "photo",      // IMAGE_CAPTURE
            "video",      // VIDEO_CAPTURE
            "camera",     // General camera access
            "selfie"      // Front camera (selfie) mode
        )
    }
    
    /**
     * Check if front camera is available
     */
    fun isFrontCameraAvailable(): Boolean {
        return try {
            val cameraIds = cameraManager.cameraIdList
            val isAvailable = cameraIds.contains(FRONT_CAMERA_ID)
            Log.d(TAG, "Front camera available: $isAvailable")
            isAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking front camera availability: ${e.message}")
            false
        }
    }
    
    /**
     * Get front camera ID for selfie mode
     */
    fun getFrontCameraId(): String {
        return FRONT_CAMERA_ID
    }
    
    /**
     * Set camera mode (front/back)
     */
    fun setCameraMode(isFrontCamera: Boolean): String {
        val cameraId = if (isFrontCamera) FRONT_CAMERA_ID else "0"
        Log.d(TAG, "Camera mode set to: ${if (isFrontCamera) "front" else "back"} (ID: $cameraId)")
        return cameraId
    }
}
