package com.app001.virtualcamera.hook

import android.content.Context
import android.util.Log
import com.app001.virtualcamera.video.VideoInfo

class NativeCameraHook(private val context: Context) {
    private val tag = "NativeCameraHook"
    private var isHooked = false

    init {
        System.loadLibrary("camera_hook")
    }

    companion object {
        init {
            System.loadLibrary("camera_hook")
        }
    }

    // Native methods
    private external fun installHooks(): Boolean
    private external fun uninstallHooks()
    private external fun loadVideo(videoPath: String): Boolean
    private external fun startVirtualCamera()
    private external fun stopVirtualCamera()
    private external fun getVideoInfo(): VideoInfo?
    
    // Advanced native methods
    private external fun installAdvancedHooks(): Boolean
    private external fun uninstallAdvancedHooks()

    fun installSystemHook(): Boolean {
        if (isHooked) {
            Log.d(tag, "Hooks already installed")
            return true
        }

        try {
            // Try advanced hooks first
            var success = installAdvancedHooks()
            if (!success) {
                // Fall back to basic hooks
                success = installHooks()
            }
            
            if (success) {
                isHooked = true
                Log.d(tag, "Native camera hooks installed successfully")
            } else {
                Log.e(tag, "Failed to install native camera hooks")
            }
            return success
        } catch (e: Exception) {
            Log.e(tag, "Exception installing native hooks: ${e.message}")
            return false
        }
    }

    fun uninstallSystemHook() {
        if (!isHooked) {
            return
        }

        try {
            // Try advanced uninstall first
            try {
                uninstallAdvancedHooks()
            } catch (e: Exception) {
                // Fall back to basic uninstall
                uninstallHooks()
            }
            
            isHooked = false
            Log.d(tag, "Native camera hooks uninstalled")
        } catch (e: Exception) {
            Log.e(tag, "Exception uninstalling native hooks: ${e.message}")
        }
    }

    fun loadVideoFile(videoPath: String): Boolean {
        return try {
            val success = loadVideo(videoPath)
            if (success) {
                Log.d(tag, "Video loaded successfully: $videoPath")
            } else {
                Log.e(tag, "Failed to load video: $videoPath")
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Exception loading video: ${e.message}")
            false
        }
    }

    fun startVirtualCameraService() {
        try {
            startVirtualCamera()
            Log.d(tag, "Virtual camera started")
        } catch (e: Exception) {
            Log.e(tag, "Exception starting virtual camera: ${e.message}")
        }
    }

    fun stopVirtualCameraService() {
        try {
            stopVirtualCamera()
            Log.d(tag, "Virtual camera stopped")
        } catch (e: Exception) {
            Log.e(tag, "Exception stopping virtual camera: ${e.message}")
        }
    }

    fun getVideoInformation(): VideoInfo? {
        return try {
            getVideoInfo()
        } catch (e: Exception) {
            Log.e(tag, "Exception getting video info: ${e.message}")
            null
        }
    }

    fun isHooked(): Boolean = isHooked
}
