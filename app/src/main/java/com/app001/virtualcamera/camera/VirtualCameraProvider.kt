package com.app001.virtualcamera.camera

import android.content.Context
import android.content.Intent
import android.util.Log
import com.app001.virtualcamera.utils.VideoPathManager

/**
 * Virtual Camera Provider that handles camera requests from other apps
 * This acts as a bridge between external apps and our virtual camera
 */
class VirtualCameraProvider {
    companion object {
        private const val TAG = "VirtualCameraProvider"
        
        /**
         * Handle camera intent from external apps
         * This method is called when other apps request camera access
         */
        fun handleCameraIntent(context: Context, intent: Intent): Intent? {
            Log.d(TAG, "Handling camera intent: ${intent.action}")
            
            // Initialize VideoPathManager
            VideoPathManager.initialize(context)
            
            // Get the current video path
            val videoPath = VideoPathManager.getCurrentVideoPath()
            
            if (videoPath == null) {
                Log.w(TAG, "No video path available, using test pattern")
            } else {
                Log.d(TAG, "Using video path: $videoPath")
            }
            
            // Create intent to launch VirtualCameraActivity
            return Intent(context, VirtualCameraActivity::class.java).apply {
                // Copy the original intent action
                action = intent.action
                
                // Add video path
                putExtra(VirtualCameraActivity.EXTRA_VIDEO_PATH, videoPath)
                putExtra(VirtualCameraActivity.EXTRA_IS_VIRTUAL_CAMERA, true)
                
                // Copy any data from original intent
                if (intent.data != null) {
                    data = intent.data
                }
                
                // Copy extras
                intent.extras?.let { extras ->
                    putExtras(extras)
                }
                
                // Set flags for proper launching
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                
                Log.d(TAG, "Created virtual camera intent with video: $videoPath")
            }
        }
        
        /**
         * Check if virtual camera is available
         */
        fun isVirtualCameraAvailable(context: Context): Boolean {
            VideoPathManager.initialize(context)
            return VideoPathManager.hasVideoPath()
        }
        
        /**
         * Get available camera modes
         */
        fun getAvailableCameraModes(): List<String> {
            return listOf(
                "photo",      // IMAGE_CAPTURE
                "video",      // VIDEO_CAPTURE
                "camera"      // General camera access
            )
        }
    }
}