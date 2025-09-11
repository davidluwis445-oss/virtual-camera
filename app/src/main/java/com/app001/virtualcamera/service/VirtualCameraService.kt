package com.app001.virtualcamera.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.hardware.camera2.*
import android.media.ImageReader
import android.graphics.SurfaceTexture
import android.view.Surface
import com.app001.virtualcamera.video.VideoStreamManager
import kotlinx.coroutines.*

class VirtualCameraService : Service() {
    private val tag = "VirtualCameraService"
    private var cameraManager: CameraManager? = null
    // private var virtualCamera: VirtualCamera? = null
    private var videoStreamManager: VideoStreamManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val ACTION_START_VIRTUAL_CAMERA = "com.app001.virtualcamera.START_VIRTUAL_CAMERA"
        const val ACTION_STOP_VIRTUAL_CAMERA = "com.app001.virtualcamera.STOP_VIRTUAL_CAMERA"
        const val ACTION_LOAD_VIDEO = "com.app001.virtualcamera.LOAD_VIDEO"
        const val EXTRA_VIDEO_PATH = "video_path"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "VirtualCameraService created")
        
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        videoStreamManager = VideoStreamManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VIRTUAL_CAMERA -> {
                startVirtualCamera()
            }
            ACTION_STOP_VIRTUAL_CAMERA -> {
                stopVirtualCamera()
            }
            ACTION_LOAD_VIDEO -> {
                val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
                if (videoPath != null) {
                    loadVideo(videoPath)
                }
            }
        }
        return START_STICKY
    }

    private fun startVirtualCamera() {
        serviceScope.launch {
            try {
                // Start video streaming
                videoStreamManager?.loadVideo("sample_video.mp4")
                Log.d(tag, "Virtual camera started successfully")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start virtual camera: ${e.message}")
            }
        }
    }

    private fun stopVirtualCamera() {
        serviceScope.launch {
            try {
                videoStreamManager?.release()
                Log.d(tag, "Virtual camera stopped")
            } catch (e: Exception) {
                Log.e(tag, "Failed to stop virtual camera: ${e.message}")
            }
        }
    }

    private fun loadVideo(videoPath: String) {
        serviceScope.launch {
            try {
                videoStreamManager?.loadVideo(videoPath)
                Log.d(tag, "Video loaded: $videoPath")
            } catch (e: Exception) {
                Log.e(tag, "Failed to load video: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVirtualCamera()
        serviceScope.cancel()
        Log.d(tag, "VirtualCameraService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
