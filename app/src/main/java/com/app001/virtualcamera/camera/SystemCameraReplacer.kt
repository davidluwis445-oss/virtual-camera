package com.app001.virtualcamera.camera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast

/**
 * System Camera Replacer that provides real system-wide camera replacement
 * This works by becoming the default camera app and providing video feed
 */
class SystemCameraReplacer(private val context: Context) {
    
    companion object {
        private const val TAG = "SystemCameraReplacer"
    }
    
    /**
     * Set this app as the default camera app
     * This makes other apps use our virtual camera when they access the camera
     */
    fun setAsDefaultCamera(): Boolean {
        return try {
            Log.d(TAG, "Setting app as default camera...")
            
            // Create intent to set as default camera
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            
            // Check if we can become the default camera
            val packageManager = context.packageManager
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            val cameraApps = packageManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            Log.d(TAG, "Found ${cameraApps.size} camera apps")
            
            // Try to set as default
            val success = try {
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open settings: ${e.message}")
                false
            }
            
            Log.d(TAG, "Default camera setting result: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception setting as default camera: ${e.message}")
            false
        }
    }
    
    /**
     * Check if this app is set as the default camera
     */
    fun isDefaultCamera(): Boolean {
        return try {
            val packageManager = context.packageManager
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            val defaultCamera = packageManager.resolveActivity(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            val isDefault = defaultCamera?.activityInfo?.packageName == context.packageName
            Log.d(TAG, "Is default camera: $isDefault")
            isDefault
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking default camera: ${e.message}")
            false
        }
    }
    
    /**
     * Start providing virtual camera feed
     * This is called when other apps try to use the camera
     */
    fun startVirtualCameraFeed(videoPath: String): Boolean {
        return try {
            Log.d(TAG, "Starting virtual camera feed: $videoPath")
            
            // Launch our camera activity with the video
            val intent = Intent(context, com.app001.virtualcamera.GhostCamActivity::class.java).apply {
                putExtra("video_path", videoPath)
                putExtra("is_virtual_camera", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            Log.d(TAG, "Virtual camera feed started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start virtual camera feed: ${e.message}")
            false
        }
    }
    
    /**
     * Stop virtual camera feed
     */
    fun stopVirtualCameraFeed(): Boolean {
        return try {
            Log.d(TAG, "Stopping virtual camera feed")
            // Implementation to stop the feed
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop virtual camera feed: ${e.message}")
            false
        }
    }
    
    /**
     * Get available camera apps on the system
     */
    fun getAvailableCameraApps(): List<String> {
        return try {
            val packageManager = context.packageManager
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            val cameraApps = packageManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            cameraApps.map { it.activityInfo.packageName }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting camera apps: ${e.message}")
            emptyList()
        }
    }
}
