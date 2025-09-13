package com.app001.virtualcamera.camera

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.app001.virtualcamera.R

/**
 * Virtual Camera Activity that provides video feed to other apps
 * This activity is launched when other apps request camera access
 */
class VirtualCameraActivity : Activity(), SurfaceHolder.Callback {
    
    companion object {
        private const val TAG = "VirtualCameraActivity"
        const val EXTRA_VIDEO_PATH = "video_path"
        const val EXTRA_IS_VIRTUAL_CAMERA = "is_virtual_camera"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var surfaceView: SurfaceView? = null
    private var videoPath: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_virtual_camera)
        
        Log.d(TAG, "Virtual Camera Activity created")
        
        // Get video path from intent
        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
        val isVirtualCamera = intent.getBooleanExtra(EXTRA_IS_VIRTUAL_CAMERA, false)
        
        Log.d(TAG, "Video path: $videoPath, Is virtual camera: $isVirtualCamera")
        
        setupUI()
        setupSurfaceView()
        
        if (isVirtualCamera) {
            startVideoPlayback()
        }
    }
    
    private fun setupUI() {
        // Setup UI elements
        val captureButton = findViewById<Button>(R.id.capture_button)
        val switchButton = findViewById<Button>(R.id.switch_button)
        val closeButton = findViewById<Button>(R.id.close_button)
        
        captureButton?.setOnClickListener {
            // Simulate photo capture
            Toast.makeText(this, "📸 Photo captured with virtual camera!", Toast.LENGTH_SHORT).show()
            
            // Return result to calling app
            val resultIntent = Intent().apply {
                putExtra("photo_captured", true)
                putExtra("photo_path", "/virtual/camera/photo_${System.currentTimeMillis()}.jpg")
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        
        switchButton?.setOnClickListener {
            Toast.makeText(this, "🔄 Camera switched (virtual camera only)", Toast.LENGTH_SHORT).show()
        }
        
        closeButton?.setOnClickListener {
            finish()
        }
    }
    
    private fun setupSurfaceView() {
        surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        surfaceView?.holder?.addCallback(this)
    }
    
    private fun startVideoPlayback() {
        try {
            mediaPlayer = MediaPlayer().apply {
                if (videoPath != null && videoPath!!.isNotEmpty()) {
                    Log.d(TAG, "Loading video from path: $videoPath")
                    setDataSource(videoPath)
                } else {
                    // Use default test video or create a test pattern
                    Log.d(TAG, "No video path provided, using test pattern")
                    Toast.makeText(this@VirtualCameraActivity, "🎥 Playing virtual camera feed (test pattern)", Toast.LENGTH_SHORT).show()
                }
                
                setDisplay(surfaceView?.holder)
                isLooping = true
                prepare()
                start()
                
                Log.d(TAG, "Video playback started successfully")
                Toast.makeText(this@VirtualCameraActivity, "✅ Virtual camera feed active", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video playback: ${e.message}")
            Toast.makeText(this, "Failed to start video: ${e.message}", Toast.LENGTH_SHORT).show()
            
            // Fallback: show a test pattern or solid color
            showTestPattern()
        }
    }
    
    private fun showTestPattern() {
        try {
            // Create a simple test pattern using a solid color
            surfaceView?.holder?.let { holder ->
                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(android.graphics.Color.BLUE)
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            Toast.makeText(this, "🎥 Showing test pattern", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show test pattern: ${e.message}")
        }
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created")
        startVideoPlayback()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: $width x $height")
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed")
        stopVideoPlayback()
    }
    
    private fun stopVideoPlayback() {
        try {
            mediaPlayer?.apply {
                stop()
                release()
            }
            mediaPlayer = null
            Log.d(TAG, "Video playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop video playback: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVideoPlayback()
    }
    
    override fun onBackPressed() {
        // Return result indicating user cancelled
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
}
