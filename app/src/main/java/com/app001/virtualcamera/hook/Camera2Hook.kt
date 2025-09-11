package com.app001.virtualcamera.hook

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import com.app001.virtualcamera.video.VideoInfo

class Camera2Hook(private val context: Context) {
    private val tag = "Camera2Hook"
    private var isHooked = false
    private var cameraManager: CameraManager? = null

    init {
        System.loadLibrary("camera2_hook")
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    companion object {
        init {
            System.loadLibrary("camera2_hook")
        }
    }

    // Native methods
    private external fun installCamera2Hook(cameraManager: CameraManager): Boolean
    private external fun uninstallCamera2Hook()
    private external fun loadVideo(videoPath: String): Boolean
    private external fun startVirtualCamera(cameraDevice: Any)
    private external fun stopVirtualCamera()
    private external fun getVideoInfo(): VideoInfo?
    private external fun isHookInstalled(): Boolean

    fun installSystemHook(): Boolean {
        if (isHooked) {
            Log.d(tag, "Camera2 hook already installed")
            return true
        }

        try {
            val success = installCamera2Hook(cameraManager!!)
            if (success) {
                isHooked = true
                Log.d(tag, "Camera2 hook installed successfully")
            } else {
                Log.e(tag, "Failed to install Camera2 hook")
            }
            return success
        } catch (e: Exception) {
            Log.e(tag, "Exception installing Camera2 hook: ${e.message}")
            return false
        }
    }

    fun uninstallSystemHook() {
        if (!isHooked) {
            return
        }

        try {
            uninstallCamera2Hook()
            isHooked = false
            Log.d(tag, "Camera2 hook uninstalled")
        } catch (e: Exception) {
            Log.e(tag, "Exception uninstalling Camera2 hook: ${e.message}")
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

    fun startVirtualCameraService(cameraDevice: Any) {
        try {
            startVirtualCamera(cameraDevice)
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
