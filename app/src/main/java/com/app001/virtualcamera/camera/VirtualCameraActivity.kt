package com.app001.virtualcamera.camera

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.app001.virtualcamera.R
import com.app001.virtualcamera.utils.VideoPathManager

/**
 * Virtual Camera Activity that provides video feed to other apps
 * This activity is launched when other apps request camera access
 */
class VirtualCameraActivity : Activity(), SurfaceHolder.Callback {
    
    companion object {
        private const val TAG = "VirtualCameraActivity"
        const val EXTRA_VIDEO_PATH = "video_path"
        const val EXTRA_IS_VIRTUAL_CAMERA = "is_virtual_camera"
        const val EXTRA_CAMERA_MODE = "camera_mode"
        const val EXTRA_IS_FRONT_CAMERA = "is_front_camera"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var surfaceView: SurfaceView? = null
    private var videoPath: String? = null
    private var isFrontCamera: Boolean = true // Default to front camera (selfie mode)
    private var cameraMode: String = "selfie" // Default to selfie mode
    
    // Simplified camera activity - no complex camera2 code
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_virtual_camera)
        
        Log.d(TAG, "Virtual Camera Activity created")
        
        // Initialize VideoPathManager
        VideoPathManager.initialize(this)
        
        // Get video path from intent or from VideoPathManager
        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: VideoPathManager.getCurrentVideoPath()
        val isVirtualCamera = intent.getBooleanExtra(EXTRA_IS_VIRTUAL_CAMERA, false)
        
        // Get camera mode settings
        isFrontCamera = intent.getBooleanExtra(EXTRA_IS_FRONT_CAMERA, true) // Default to front camera
        cameraMode = intent.getStringExtra(EXTRA_CAMERA_MODE) ?: "selfie" // Default to selfie mode
        
        // Check if this is a camera intent from external app
        val isCameraIntent = intent.action == android.provider.MediaStore.ACTION_IMAGE_CAPTURE || 
                            intent.action == android.provider.MediaStore.ACTION_VIDEO_CAPTURE
        
        Log.d(TAG, "Video path: $videoPath, Is virtual camera: $isVirtualCamera, Is camera intent: $isCameraIntent")
        Log.d(TAG, "Camera mode: $cameraMode, Is front camera: $isFrontCamera")
        
        setupUI()
        setupSurfaceView()
        
        // Always start video playback for external camera apps
        if (isVirtualCamera || isCameraIntent) {
            startVideoPlayback()
            
            // For external camera apps, hide the UI and show full screen video
            if (isCameraIntent) {
                hideCameraUI()
                setupTapToCapture()
            }
        }
    }
    
    // Simplified setup - no complex camera permissions needed
    
    private fun setupUI() {
        // Setup UI elements
        val captureButton = findViewById<Button>(R.id.capture_button)
        val switchButton = findViewById<Button>(R.id.switch_button)
        val closeButton = findViewById<Button>(R.id.close_button)
        
        captureButton?.setOnClickListener {
            // Simulate photo capture
            Toast.makeText(this, "📸 Photo captured with virtual camera!", Toast.LENGTH_SHORT).show()
            
            // Return result to calling app with proper data URI
            val resultIntent = Intent().apply {
                // For image capture, return a data URI
                data = android.net.Uri.parse("content://media/external/images/media/${System.currentTimeMillis()}")
                putExtra("photo_captured", true)
                putExtra("photo_path", "/virtual/camera/photo_${System.currentTimeMillis()}.jpg")
                putExtra("data", android.net.Uri.parse("content://media/external/images/media/${System.currentTimeMillis()}"))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        
        switchButton?.setOnClickListener {
            // Toggle between front and back camera
            isFrontCamera = !isFrontCamera
            cameraMode = if (isFrontCamera) "selfie" else "back"
            
            val cameraText = if (isFrontCamera) "Front Camera (Selfie)" else "Back Camera"
            Toast.makeText(this, "🔄 Switched to $cameraText", Toast.LENGTH_SHORT).show()
            
            // Update camera status text
            updateCameraStatusText()
            
            Log.d(TAG, "Camera switched to: $cameraMode (Front: $isFrontCamera)")
        }
        
        closeButton?.setOnClickListener {
            finish()
        }
        
        // Update initial camera status text
        updateCameraStatusText()
    }
    
    private fun updateCameraStatusText() {
        try {
            val statusText = findViewById<TextView>(R.id.camera_status_text)
            val cameraText = if (isFrontCamera) "Front Camera (Selfie)" else "Back Camera"
            val modeText = if (cameraMode == "selfie") "Selfie Mode" else "Camera Mode"
            statusText?.text = "🎥 Virtual Camera Active - $cameraText ($modeText)"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating camera status text: ${e.message}")
        }
    }
    
    private fun setupSurfaceView() {
        surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        surfaceView?.holder?.addCallback(this)
    }
    
    // Simplified camera handling - using MediaPlayer for video display
    
    private fun hideCameraUI() {
        try {
            // Hide the camera controls for external apps
            val controlsLayout = findViewById<LinearLayout>(R.id.camera_controls)
            controlsLayout?.visibility = View.GONE
            
            // Make the surface view full screen
            surfaceView?.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            
            // Hide system UI for immersive experience
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            
            Log.d(TAG, "Camera UI hidden for external app")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding camera UI: ${e.message}")
        }
    }
    
    private fun setupTapToCapture() {
        try {
            // Make the surface view clickable for tap-to-capture
            surfaceView?.setOnClickListener {
                // Simulate photo capture when user taps the screen
                Toast.makeText(this, "📸 Photo captured!", Toast.LENGTH_SHORT).show()
                
                // Return result to calling app
                val resultIntent = Intent().apply {
                    data = android.net.Uri.parse("content://media/external/images/media/${System.currentTimeMillis()}")
                    putExtra("photo_captured", true)
                    putExtra("photo_path", "/virtual/camera/photo_${System.currentTimeMillis()}.jpg")
                    putExtra("data", android.net.Uri.parse("content://media/external/images/media/${System.currentTimeMillis()}"))
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            
            Log.d(TAG, "Tap-to-capture setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up tap-to-capture: ${e.message}")
        }
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
