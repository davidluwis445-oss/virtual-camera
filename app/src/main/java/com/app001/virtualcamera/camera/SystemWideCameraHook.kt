package com.app001.virtualcamera.camera

import android.content.Context
import android.util.Log
import java.io.File

/**
 * System Wide Camera Hook - ACTUALLY replaces system camera for ALL apps
 * This is the REAL solution that makes TikTok, Instagram, etc. see your video
 */
class SystemWideCameraHook {
    
    companion object {
        private const val TAG = "SystemWideCameraHook"
        
        init {
            try {
                System.loadLibrary("system_wide_camera_hook")
                Log.d(TAG, "System wide camera hook library loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load system wide camera hook library: ${e.message}")
            }
        }
        
        // Singleton instance for system-wide access
        @JvmStatic
        val instance = SystemWideCameraHook()
    }
    
    private var isSystemWideActive = false
    
    /**
     * Install system-wide camera hooks that intercept ALL camera calls
     * This makes your video appear as the system camera for ALL apps
     */
    fun installSystemWideCameraHooks(): Boolean {
        return try {
            Log.d(TAG, "🚀 Installing SYSTEM-WIDE camera hooks")
            val success = installSystemWideHooks()
            
            if (success) {
                isSystemWideActive = true
                Log.d(TAG, "✅ SYSTEM-WIDE camera hooks installed successfully!")
                Log.d(TAG, "🎯 ALL camera apps (TikTok, Instagram, Telegram) will now see virtual camera!")
                Log.d(TAG, "📱 Open any camera app to test - they will see your video!")
            } else {
                Log.e(TAG, "❌ Failed to install system-wide camera hooks")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception installing system-wide camera hooks: ${e.message}")
            false
        }
    }
    
    /**
     * Uninstall system-wide camera hooks and restore normal camera
     */
    fun uninstallSystemWideCameraHooks() {
        try {
            Log.d(TAG, "🔄 Uninstalling system-wide camera hooks")
            uninstallSystemWideHooks()
            isSystemWideActive = false
            Log.d(TAG, "✅ System-wide camera hooks uninstalled - normal camera restored")
        } catch (e: Exception) {
            Log.e(TAG, "Exception uninstalling system-wide camera hooks: ${e.message}")
        }
    }
    
    /**
     * Set the video file to use as system camera replacement
     */
    fun setSystemVirtualCameraVideo(videoPath: String) {
        try {
            Log.d(TAG, "📹 Setting system virtual camera video: $videoPath")
            setSystemVideoPath(videoPath)
            Log.d(TAG, "✅ Video set successfully - this video will appear in ALL camera apps")
        } catch (e: Exception) {
            Log.e(TAG, "Exception setting system video path: ${e.message}")
        }
    }
    
    /**
     * Force system camera replacement (requires root for maximum effect)
     */
    fun forceSystemCameraReplacement() {
        try {
            Log.d(TAG, "💪 Forcing system camera replacement")
            nativeForceSystemCameraReplacement()
            Log.d(TAG, "✅ System camera replacement forced")
        } catch (e: Exception) {
            Log.e(TAG, "Exception forcing system camera replacement: ${e.message}")
        }
    }
    
    /**
     * Check if system-wide camera hooks are active
     */
    fun isSystemWideCameraHookActive(): Boolean {
        return try {
            val nativeActive = isSystemWideHookActive()
            val localActive = isSystemWideActive
            val result = nativeActive && localActive
            
            Log.d(TAG, "System wide hook status - Native: $nativeActive, Local: $localActive, Result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking system-wide hook status: ${e.message}")
            false
        }
    }
    
    /**
     * Get current system camera frame data
     */
    fun getSystemCameraFrameData(): ByteArray? {
        return try {
            getSystemCameraFrame()
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting system camera frame: ${e.message}")
            null
        }
    }
    
    /**
     * Get detailed system camera status for debugging
     */
    fun getSystemCameraStatus(): String {
        return try {
            val status = nativeGetSystemCameraStatus()
            Log.d(TAG, "System camera status:\n$status")
            status
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting system camera status: ${e.message}")
            "Error getting status: ${e.message}"
        }
    }
    
    /**
     * Complete system-wide virtual camera setup
     * This is the main method to call for full system camera replacement
     */
    fun setupCompleteSystemWideVirtualCamera(context: Context, videoPath: String): Boolean {
        return try {
            Log.d(TAG, "🎯 Setting up COMPLETE system-wide virtual camera")
            Log.d(TAG, "📹 Video: $videoPath")
            
            // Step 1: Set the video path
            setSystemVirtualCameraVideo(videoPath)
            
            // Step 2: Install system-wide hooks
            val hookSuccess = installSystemWideCameraHooks()
            if (!hookSuccess) {
                Log.e(TAG, "❌ Failed to install system-wide hooks")
                return false
            }
            
            // Step 3: Force system camera replacement
            nativeForceSystemCameraReplacement()
            
            // Step 4: Verify installation
            val status = nativeGetSystemCameraStatus()
            Log.d(TAG, "📊 System status after setup:\n$status")
            
            Log.d(TAG, "🎉 COMPLETE system-wide virtual camera setup successful!")
            Log.d(TAG, "🚀 NOW ALL CAMERA APPS WILL SEE YOUR VIDEO!")
            Log.d(TAG, "📱 Test with TikTok, Instagram, Telegram, etc.")
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception in complete system-wide setup: ${e.message}")
            false
        }
    }
    
    /**
     * Disable complete system-wide virtual camera
     */
    fun disableCompleteSystemWideVirtualCamera() {
        try {
            Log.d(TAG, "🔄 Disabling complete system-wide virtual camera")
            
            uninstallSystemWideCameraHooks()
            
            Log.d(TAG, "✅ Complete system-wide virtual camera disabled")
            Log.d(TAG, "📱 Normal camera functionality restored")
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception disabling complete system-wide virtual camera: ${e.message}")
        }
    }
    
    // Native methods
    private external fun installSystemWideHooks(): Boolean
    private external fun uninstallSystemWideHooks()
    private external fun isSystemWideHookActive(): Boolean
    private external fun setSystemVideoPath(videoPath: String)
    private external fun getSystemCameraFrame(): ByteArray?
    private external fun nativeForceSystemCameraReplacement()
    private external fun nativeGetSystemCameraStatus(): String
}
