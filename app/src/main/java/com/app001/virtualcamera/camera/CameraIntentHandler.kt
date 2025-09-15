package com.app001.virtualcamera.camera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import com.app001.virtualcamera.utils.VideoPathManager

/**
 * Camera Intent Handler that manages camera app selection
 * This ensures our virtual camera is used by third-party apps
 */
object CameraIntentHandler {
    private const val TAG = "CameraIntentHandler"
    
    /**
     * Check if our app can handle camera intents
     */
    fun canHandleCameraIntent(context: Context, intent: Intent): Boolean {
        return try {
            val packageManager = context.packageManager
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            
            val canHandle = resolveInfo?.activityInfo?.packageName == context.packageName
            Log.d(TAG, "Can handle camera intent: $canHandle")
            canHandle
        } catch (e: Exception) {
            Log.e(TAG, "Error checking camera intent handling: ${e.message}")
            false
        }
    }
    
    /**
     * Get all camera apps that can handle the intent
     */
    fun getCameraApps(context: Context, intent: Intent): List<ResolveInfo> {
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
    fun setAsPreferredCameraApp(context: Context): Boolean {
        return try {
            Log.d(TAG, "Setting as preferred camera app")
            
            // Create camera intent
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            
            // Get all camera apps
            val cameraApps = getCameraApps(context, cameraIntent)
            
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
    
    /**
     * Force launch our virtual camera for a given intent
     */
    fun forceLaunchVirtualCamera(context: Context, intent: Intent): Boolean {
        return try {
            Log.d(TAG, "Force launching virtual camera")
            
            // Initialize VideoPathManager
            VideoPathManager.initialize(context)
            
            // Get video path
            val videoPath = VideoPathManager.getCurrentVideoPath()
            
            // Create virtual camera intent
            val virtualCameraIntent = Intent(context, VirtualCameraActivity::class.java).apply {
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
            context.startActivity(virtualCameraIntent)
            Log.d(TAG, "Virtual camera launched successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error force launching virtual camera: ${e.message}")
            false
        }
    }
    
    /**
     * Check if virtual camera is ready
     */
    fun isVirtualCameraReady(context: Context): Boolean {
        return try {
            VideoPathManager.initialize(context)
            val hasVideoPath = VideoPathManager.hasVideoPath()
            Log.d(TAG, "Virtual camera ready: $hasVideoPath")
            hasVideoPath
        } catch (e: Exception) {
            Log.e(TAG, "Error checking virtual camera readiness: ${e.message}")
            false
        }
    }
}
