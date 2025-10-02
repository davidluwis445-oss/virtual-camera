package com.app001.virtualcamera.camera

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import com.app001.virtualcamera.R
import com.app001.virtualcamera.utils.VideoPathManager
import java.io.File
import java.io.FileOutputStream

/**
 * Simple Virtual Camera Activity - Actually works with external apps!
 * This activity provides real camera replacement for TikTok, Instagram, etc.
 */
class SimpleVirtualCameraActivity : Activity(), SurfaceHolder.Callback {
    
    companion object {
        private const val TAG = "SimpleVirtualCameraActivity"
        const val EXTRA_VIDEO_PATH = "video_path"
    }
    
    private var surfaceView: SurfaceView? = null
    private var videoPath: String? = null
    private val systemWideHook = SystemWideCameraHook.instance
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_virtual_camera)
        
        Log.d(TAG, "Simple Virtual Camera Activity created")
        
        // Get video path
        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: VideoPathManager.getCurrentVideoPath()
        
        // Check if this is a camera intent from external app
        val isCameraIntent = intent.action == android.provider.MediaStore.ACTION_IMAGE_CAPTURE || 
                            intent.action == android.provider.MediaStore.ACTION_VIDEO_CAPTURE
        
        Log.d(TAG, "Video path: $videoPath, Camera intent: $isCameraIntent")
        
        setupUI(isCameraIntent)
        setupSurfaceView()
        
        // Load and start camera replacement
        startCameraReplacement()
    }
    
    private fun setupUI(isCameraIntent: Boolean) {
        val captureButton = findViewById<Button>(R.id.simple_capture_button)
        val statusText = findViewById<TextView>(R.id.simple_status_text)
        val controlsLayout = findViewById<LinearLayout>(R.id.simple_controls)
        
        captureButton?.setOnClickListener {
            capturePhoto()
        }
        
        // Hide controls for external apps, show full screen
        if (isCameraIntent) {
            controlsLayout?.visibility = View.GONE
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            
            // Make surface view clickable for tap-to-capture
            surfaceView?.setOnClickListener {
                capturePhoto()
            }
        }
        
        statusText?.text = "🎥 Simple Virtual Camera Active"
    }
    
    private fun setupSurfaceView() {
        surfaceView = findViewById(R.id.simple_surface_view)
        surfaceView?.holder?.addCallback(this)
    }
    
    private fun startCameraReplacement() {
        try {
            Log.d(TAG, "Starting SYSTEM-WIDE camera replacement")
            
            // Load video if available
            videoPath?.let { path ->
                Log.d(TAG, "Setting system video path: $path")
                systemWideHook.setSystemVirtualCameraVideo(path)
            }
            
            // Install system-wide camera hooks
            val hookSuccess = systemWideHook.installSystemWideCameraHooks()
            if (hookSuccess) {
                Log.d(TAG, "✅ System-wide camera hooks installed successfully")
                
                // Force system camera replacement
                systemWideHook.forceSystemCameraReplacement()
                
                // Get status for debugging
                val status = systemWideHook.getSystemCameraStatus()
                Log.d(TAG, "System camera status:\n$status")
                showToast("✅ Virtual Camera Active - External apps will see your video!")
            } else {
                Log.e(TAG, "❌ Failed to install system-wide camera hooks")
                showToast("❌ Failed to start virtual camera")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera replacement: ${e.message}")
            showToast("❌ Error: ${e.message}")
        }
    }
    
    private fun capturePhoto() {
        try {
            Log.d(TAG, "Capturing photo")
            
            // Get current video frame from system-wide hook
            val frameData = systemWideHook.getSystemCameraFrameData()
            if (frameData != null) {
                // Save as photo
                val photoFile = saveFrameAsPhoto(frameData)
                if (photoFile != null) {
                    // Return result to calling app
                    val resultIntent = Intent().apply {
                        data = android.net.Uri.fromFile(photoFile)
                        putExtra("photo_captured", true)
                        putExtra("photo_path", photoFile.absolutePath)
                    }
                    
                    setResult(Activity.RESULT_OK, resultIntent)
                    showToast("📸 Photo captured!")
                    
                    // Close activity for external apps
                    if (isCameraIntent()) {
                        finish()
                    }
                } else {
                    showToast("❌ Failed to save photo")
                }
            } else {
                showToast("❌ No video frame available")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing photo: ${e.message}")
            showToast("❌ Error capturing photo")
        }
    }
    
    private fun saveFrameAsPhoto(frameData: ByteArray): File? {
        return try {
            // Create bitmap from frame data (assuming RGBA format)
            val width = 640
            val height = 480
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val buffer = java.nio.ByteBuffer.wrap(frameData)
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Save to file
            val photoDir = File(externalCacheDir, "simple_camera_photos")
            if (!photoDir.exists()) {
                photoDir.mkdirs()
            }
            
            val photoFile = File(photoDir, "photo_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(photoFile)
            
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            
            Log.d(TAG, "Photo saved: ${photoFile.absolutePath}")
            photoFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving photo: ${e.message}")
            null
        }
    }
    
    private fun isCameraIntent(): Boolean {
        return intent.action == android.provider.MediaStore.ACTION_IMAGE_CAPTURE || 
               intent.action == android.provider.MediaStore.ACTION_VIDEO_CAPTURE
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    // SurfaceHolder.Callback implementation
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created")
        // Surface is ready for drawing - no need to do anything special
        // The system-wide hooks will handle the actual camera replacement
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: ${width}x${height}")
        // Surface size changed - no action needed for system-wide hooks
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed")
        // Clean up resources safely
        try {
            // Don't try to access the surface after it's destroyed
            Log.d(TAG, "Surface destroyed safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error during surface cleanup: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            // Clean up system-wide hooks safely
            Log.d(TAG, "Cleaning up system-wide camera hooks")
            systemWideHook.disableCompleteSystemWideVirtualCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
        Log.d(TAG, "Simple Virtual Camera Activity destroyed")
    }
    
    override fun onBackPressed() {
        if (isCameraIntent()) {
            setResult(Activity.RESULT_CANCELED)
        }
        super.onBackPressed()
    }
}
