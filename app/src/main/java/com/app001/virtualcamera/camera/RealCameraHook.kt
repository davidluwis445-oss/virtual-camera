package com.app001.virtualcamera.camera

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Real Camera Hook - Actually replaces system camera for ALL apps
 * This hooks into the system camera at a lower level
 */
class RealCameraHook {
    
    companion object {
        private const val TAG = "RealCameraHook"
        
        init {
            try {
                System.loadLibrary("real_camera_hook")
                Log.d(TAG, "Real camera hook library loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load real camera hook library: ${e.message}")
            }
        }
        
        // Singleton instance
        @JvmStatic
        val instance = RealCameraHook()
    }
    
    private var isActive = false
    
    /**
     * Install real camera hooks that intercept system camera calls
     */
    fun installRealCameraHooks(): Boolean {
        return try {
            Log.d(TAG, "Installing real camera hooks")
            val success = installHooks()
            
            if (success) {
                isActive = true
                Log.d(TAG, "Real camera hooks installed successfully")
                Log.d(TAG, "🎯 ALL camera apps will now see virtual camera!")
            } else {
                Log.e(TAG, "Failed to install real camera hooks")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception installing real camera hooks: ${e.message}")
            false
        }
    }
    
    /**
     * Uninstall real camera hooks
     */
    fun uninstallRealCameraHooks() {
        try {
            Log.d(TAG, "Uninstalling real camera hooks")
            uninstallHooks()
            isActive = false
            Log.d(TAG, "Real camera hooks uninstalled")
        } catch (e: Exception) {
            Log.e(TAG, "Exception uninstalling real camera hooks: ${e.message}")
        }
    }
    
    /**
     * Set the video file to use as fake camera feed
     */
    fun setVirtualCameraVideo(videoPath: String) {
        try {
            Log.d(TAG, "Setting virtual camera video: $videoPath")
            setVideoPath(videoPath)
        } catch (e: Exception) {
            Log.e(TAG, "Exception setting video path: ${e.message}")
        }
    }
    
    /**
     * Check if real camera hooks are active
     */
    fun isRealCameraHookActive(): Boolean {
        return try {
            isHookActive() && isActive
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking hook status: ${e.message}")
            false
        }
    }
    
    /**
     * Get current fake camera frame
     */
    fun getCurrentCameraFrame(): ByteArray? {
        return try {
            getCurrentFrame()
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting current frame: ${e.message}")
            null
        }
    }
    
    /**
     * Enable system-wide virtual camera mode
     */
    fun enableSystemWideVirtualCamera(context: Context, videoPath: String): Boolean {
        return try {
            Log.d(TAG, "Enabling system-wide virtual camera mode")
            
            // Set the video path
            setVirtualCameraVideo(videoPath)
            
            // Install hooks
            val hookSuccess = installRealCameraHooks()
            
            if (hookSuccess) {
                Log.d(TAG, "✅ System-wide virtual camera enabled!")
                Log.d(TAG, "🎯 TikTok, Telegram, Instagram will now see your video!")
                
                // Set additional system properties for maximum compatibility
                try {
                    Runtime.getRuntime().exec("setprop persist.camera.virtual.enabled 1")
                    Runtime.getRuntime().exec("setprop debug.camera.fake 1")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set system properties (root required): ${e.message}")
                }
                
                return true
            } else {
                Log.e(TAG, "❌ Failed to enable system-wide virtual camera")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception enabling system-wide virtual camera: ${e.message}")
            false
        }
    }
    
    /**
     * Disable system-wide virtual camera mode
     */
    fun disableSystemWideVirtualCamera() {
        try {
            Log.d(TAG, "Disabling system-wide virtual camera mode")
            
            uninstallRealCameraHooks()
            
            // Reset system properties
            try {
                Runtime.getRuntime().exec("setprop persist.camera.virtual.enabled 0")
                Runtime.getRuntime().exec("setprop debug.camera.fake 0")
            } catch (e: Exception) {
                Log.w(TAG, "Could not reset system properties: ${e.message}")
            }
            
            Log.d(TAG, "System-wide virtual camera disabled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception disabling system-wide virtual camera: ${e.message}")
        }
    }
    
    // Native methods
    private external fun installHooks(): Boolean
    private external fun uninstallHooks()
    private external fun isHookActive(): Boolean
    private external fun setVideoPath(videoPath: String)
    private external fun getCurrentFrame(): ByteArray?
}
