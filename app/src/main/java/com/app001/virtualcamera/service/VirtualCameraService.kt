package com.app001.virtualcamera.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import java.io.File

/**
 * Virtual Camera Service that provides system-wide camera replacement
 * This service runs in the background and provides video feed to other apps
 */
class VirtualCameraService : Service() {
    
    companion object {
        private const val TAG = "VirtualCameraService"
        const val ACTION_START_CAMERA = "com.app001.virtualcamera.START_CAMERA"
        const val ACTION_STOP_CAMERA = "com.app001.virtualcamera.STOP_CAMERA"
        const val EXTRA_VIDEO_PATH = "video_path"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var isRunning = false
    private var videoPath: String? = null
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CAMERA -> {
                val path = intent.getStringExtra(EXTRA_VIDEO_PATH)
                startVirtualCamera(path)
            }
            ACTION_STOP_CAMERA -> {
                stopVirtualCamera()
            }
        }
        return START_STICKY
    }
    
    private fun startVirtualCamera(videoPath: String?) {
        if (isRunning) {
            Log.w(TAG, "Virtual camera is already running")
            return
        }
        
        this.videoPath = videoPath
        Log.d(TAG, "Starting virtual camera with video: $videoPath")
        
        try {
            // Initialize MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                if (videoPath != null && File(videoPath).exists()) {
                    setDataSource(videoPath)
                } else {
                    // Use default test video from assets
                    val afd = assets.openFd("test_video.mp4")
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                
                isLooping = true
                prepare()
                
                // Create SurfaceTexture for video output
                surfaceTexture = SurfaceTexture(0).apply {
                    setDefaultBufferSize(1280, 720)
                }
                
                val surface = Surface(surfaceTexture)
                setSurface(surface)
                
                start()
                isRunning = true
                
                Log.d(TAG, "Virtual camera started successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start virtual camera: ${e.message}")
        }
    }
    
    private fun stopVirtualCamera() {
        if (!isRunning) {
            Log.w(TAG, "Virtual camera is not running")
            return
        }
        
        Log.d(TAG, "Stopping virtual camera")
        
        try {
            mediaPlayer?.apply {
                stop()
                release()
            }
            surfaceTexture?.release()
            
            mediaPlayer = null
            surfaceTexture = null
            isRunning = false
            
            Log.d(TAG, "Virtual camera stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop virtual camera: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVirtualCamera()
    }
    
    /**
     * Get the current video frame as a byte array
     * This can be used by other apps to get the virtual camera feed
     */
    fun getCurrentFrame(): ByteArray? {
        return try {
            // This is a simplified implementation
            // In a real implementation, you would capture the current frame
            // from the MediaPlayer or SurfaceTexture
            ByteArray(1280 * 720 * 3) // Placeholder for RGB frame data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current frame: ${e.message}")
            null
        }
    }
    
    /**
     * Check if the virtual camera is currently running
     */
    fun isVirtualCameraRunning(): Boolean = isRunning
}
