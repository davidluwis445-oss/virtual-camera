package com.app001.virtualcamera.camera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import com.app001.virtualcamera.service.SystemCameraService

/**
 * System Camera Interceptor that intercepts camera requests from all apps
 * This ensures our virtual camera is used instead of the system camera
 */
class SystemCameraInterceptor(private val context: Context) {
    companion object {
        private const val TAG = "SystemCameraInterceptor"
    }
    
    /**
     * Intercept camera intent and redirect to virtual camera
     */
    fun interceptCameraIntent(intent: Intent): Boolean {
        return try {
            Log.d(TAG, "Intercepting camera intent: ${intent.action}")
            
            // Check if this is a camera intent
            if (!isCameraIntent(intent)) {
                Log.d(TAG, "Not a camera intent, ignoring")
                return false
            }
            
            // Check if virtual camera is available
            if (!isVirtualCameraAvailable()) {
                Log.w(TAG, "Virtual camera not available, cannot intercept")
                return false
            }
            
            // Redirect to virtual camera
            redirectToVirtualCamera(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error intercepting camera intent: ${e.message}")
            false
        }
    }
    
    /**
     * Check if the intent is a camera intent
     */
    private fun isCameraIntent(intent: Intent): Boolean {
        val action = intent.action
        return action == android.provider.MediaStore.ACTION_IMAGE_CAPTURE ||
                action == android.provider.MediaStore.ACTION_VIDEO_CAPTURE ||
                action == "android.intent.action.CAMERA" ||
                action == "android.intent.action.CAMERA_BUTTON"
    }
    
    /**
     * Check if virtual camera is available
     */
    private fun isVirtualCameraAvailable(): Boolean {
        return try {
            // Check if our app can handle camera intents
            val packageManager = context.packageManager
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            val resolveInfo = packageManager.resolveActivity(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
            
            val isAvailable = resolveInfo?.activityInfo?.packageName == context.packageName
            Log.d(TAG, "Virtual camera available: $isAvailable")
            isAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking virtual camera availability: ${e.message}")
            false
        }
    }
    
    /**
     * Redirect camera intent to virtual camera
     */
    private fun redirectToVirtualCamera(intent: Intent): Boolean {
        return try {
            Log.d(TAG, "Redirecting to virtual camera")
            
            // Start system camera service if not running
            val serviceIntent = Intent(context, SystemCameraService::class.java).apply {
                action = SystemCameraService.ACTION_START_SYSTEM_CAMERA
            }
            context.startService(serviceIntent)
            
            // Handle the camera intent
            val systemCameraService = SystemCameraService()
            val success = systemCameraService.handleCameraIntent(intent)
            
            if (success) {
                Log.d(TAG, "Successfully redirected to virtual camera")
            } else {
                Log.e(TAG, "Failed to redirect to virtual camera")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error redirecting to virtual camera: ${e.message}")
            false
        }
    }
    
    /**
     * Get all camera apps that can handle the intent
     */
    fun getCameraApps(intent: Intent): List<ResolveInfo> {
        return try {
            val packageManager = context.packageManager
            val resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            
            Log.d(TAG, "Found ${resolveInfoList.size} camera apps")
            resolveInfoList.forEach { info ->
                Log.d(TAG, "Camera app: ${info.activityInfo.packageName}")
            }
            
            resolveInfoList
        } catch (e: Exception) {
            Log.e(TAG, "Error getting camera apps: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Set our app as the preferred camera app
     */
    fun setAsPreferredCameraApp(): Boolean {
        return try {
            Log.d(TAG, "Setting as preferred camera app")
            
            // Create camera intent
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            
            // Get all camera apps
            val cameraApps = getCameraApps(cameraIntent)
            
            // Check if our app is in the list
            val ourApp = cameraApps.find { 
                it.activityInfo.packageName == context.packageName 
            }
            
            if (ourApp != null) {
                Log.d(TAG, "Our app found in camera apps list")
                
                // Try to set as default using system settings
                val settingsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
                
                Log.d(TAG, "Opened settings to set as default camera app")
                true
            } else {
                Log.w(TAG, "Our app not found in camera apps list")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting as preferred camera app: ${e.message}")
            false
        }
    }
}
