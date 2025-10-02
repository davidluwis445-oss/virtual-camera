package com.app001.virtualcamera.camera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import com.app001.virtualcamera.utils.VideoPathManager

/**
 * Camera Intent Handler that manages preview replacement
 * This ensures our video replaces camera preview while keeping camera functionality
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
     * Enable preview replacement mode
     * This allows our video to replace camera preview without disabling camera functionality
     */
    fun enablePreviewReplacement(context: Context): Boolean {
        return try {
            Log.d(TAG, "Enabling preview replacement mode")
            
            // Initialize VideoPathManager
            VideoPathManager.initialize(context)
            
            // Check if we have a video path
            val hasVideoPath = VideoPathManager.hasVideoPath()
            if (!hasVideoPath) {
                Log.w(TAG, "No video path available for preview replacement")
                return false
            }
            
            Log.d(TAG, "Preview replacement mode enabled successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling preview replacement: ${e.message}")
            false
        }
    }
    
    /**
     * Launch preview replacement for a given camera intent
     */
    fun launchPreviewReplacement(context: Context, intent: Intent): Boolean {
        return try {
            Log.d(TAG, "Launching preview replacement")
            
            // Initialize VideoPathManager
            VideoPathManager.initialize(context)
            
            // Get video path
            val videoPath = VideoPathManager.getCurrentVideoPath()
            
            // Create preview replacement intent
            val previewIntent = Intent(context, SimpleVirtualCameraActivity::class.java).apply {
                // Copy original intent action
                action = intent.action
                
                // Add video path for preview replacement
                putExtra(SimpleVirtualCameraActivity.EXTRA_VIDEO_PATH, videoPath)
                
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
            
            // Launch preview replacement
            context.startActivity(previewIntent)
            Log.d(TAG, "Preview replacement launched successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error launching preview replacement: ${e.message}")
            false
        }
    }
    
    /**
     * Check if preview replacement is ready
     */
    fun isPreviewReplacementReady(context: Context): Boolean {
        return try {
            VideoPathManager.initialize(context)
            val hasVideoPath = VideoPathManager.hasVideoPath()
            Log.d(TAG, "Preview replacement ready: $hasVideoPath")
            hasVideoPath
        } catch (e: Exception) {
            Log.e(TAG, "Error checking preview replacement readiness: ${e.message}")
            false
        }
    }
}
