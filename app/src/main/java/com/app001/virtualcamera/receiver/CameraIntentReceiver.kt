package com.app001.virtualcamera.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.app001.virtualcamera.camera.SimpleVirtualCameraActivity

/**
 * Broadcast Receiver that intercepts camera intents from other apps
 * This ensures our virtual camera is used when other apps request camera access
 */
class CameraIntentReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CameraIntentReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_CAMERA_BUTTON,
            android.provider.MediaStore.ACTION_IMAGE_CAPTURE,
            android.provider.MediaStore.ACTION_VIDEO_CAPTURE -> {
                handleCameraIntent(context, intent)
            }
        }
    }
    
    private fun handleCameraIntent(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "Handling camera intent: ${intent.action}")
            
            // Create simple virtual camera intent
            val virtualCameraIntent = Intent(context, SimpleVirtualCameraActivity::class.java).apply {
                // Copy original intent action
                action = intent.action
                
                // Copy any data from original intent
                if (intent.data != null) {
                    data = intent.data
                }
                intent.extras?.let { extras ->
                    putExtras(extras)
                }
                
                // Set flags for proper launching
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            // Launch simple virtual camera
            context.startActivity(virtualCameraIntent)
            Log.d(TAG, "Simple virtual camera launched successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling camera intent: ${e.message}")
        }
    }
}
