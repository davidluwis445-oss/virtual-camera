package com.app001.virtualcamera.service

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.File

/**
 * Video Feed Service that provides continuous video feed to other apps
 * This service runs in the background and provides video frames
 */
class VideoFeedService : Service(), SurfaceHolder.Callback {
    
    companion object {
        private const val TAG = "VideoFeedService"
        const val ACTION_START_FEED = "com.app001.virtualcamera.START_FEED"
        const val ACTION_STOP_FEED = "com.app001.virtualcamera.STOP_FEED"
        const val EXTRA_VIDEO_PATH = "video_path"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var surfaceView: SurfaceView? = null
    private var isRunning = false
    private var videoPath: String? = null
    private var currentFrame: Bitmap? = null
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FEED -> {
                val path = intent.getStringExtra(EXTRA_VIDEO_PATH)
                startVideoFeed(path)
            }
            ACTION_STOP_FEED -> {
                stopVideoFeed()
            }
        }
        return START_STICKY
    }
    
    private fun startVideoFeed(videoPath: String?) {
        if (isRunning) {
            Log.w(TAG, "Video feed is already running")
            return
        }
        
        this.videoPath = videoPath
        Log.d(TAG, "Starting video feed with video: $videoPath")
        
        try {
            // Initialize MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                if (videoPath != null && File(videoPath).exists()) {
                    Log.d(TAG, "Loading video from file: $videoPath")
                    setDataSource(videoPath)
                } else {
                    Log.d(TAG, "No valid video path, using test pattern")
                    // Create a test pattern instead
                    createTestPattern()
                    return
                }
                
                isLooping = true
                prepare()
                start()
                
                isRunning = true
                Log.d(TAG, "Video feed started successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video feed: ${e.message}")
            // Fallback to test pattern
            createTestPattern()
        }
    }
    
    private fun createTestPattern() {
        try {
            Log.d(TAG, "Creating test pattern")
            // Create a simple test pattern
            val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            
            // Draw a test pattern
            paint.color = Color.BLUE
            canvas.drawRect(0f, 0f, 640f, 360f, paint)
            
            paint.color = Color.RED
            canvas.drawRect(640f, 0f, 1280f, 360f, paint)
            
            paint.color = Color.GREEN
            canvas.drawRect(0f, 360f, 640f, 720f, paint)
            
            paint.color = Color.YELLOW
            canvas.drawRect(640f, 360f, 1280f, 720f, paint)
            
            // Add text
            paint.color = Color.WHITE
            paint.textSize = 48f
            canvas.drawText("Virtual Camera Feed", 100f, 100f, paint)
            
            currentFrame = bitmap
            isRunning = true
            
            Log.d(TAG, "Test pattern created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create test pattern: ${e.message}")
        }
    }
    
    private fun stopVideoFeed() {
        if (!isRunning) {
            Log.w(TAG, "Video feed is not running")
            return
        }
        
        Log.d(TAG, "Stopping video feed")
        
        try {
            mediaPlayer?.apply {
                stop()
                release()
            }
            
            currentFrame = null
            isRunning = false
            
            Log.d(TAG, "Video feed stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop video feed: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVideoFeed()
    }
    
    /**
     * Get the current video frame as a bitmap
     * This can be used by other apps to get the virtual camera feed
     */
    fun getCurrentFrame(): Bitmap? {
        return currentFrame
    }
    
    /**
     * Check if video feed is currently running
     */
    fun isVideoFeedRunning(): Boolean = isRunning
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created")
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: $width x $height")
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed")
    }
}
