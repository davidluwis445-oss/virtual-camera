package com.app001.virtualcamera.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.app001.virtualcamera.camera.VirtualCameraProvider

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
            
            // Check if virtual camera is available
            if (!VirtualCameraProvider.isVirtualCameraAvailable(context)) {
                Log.w(TAG, "Virtual camera not available, skipping")
                return
            }
            
            // Create virtual camera intent
            val virtualCameraIntent = VirtualCameraProvider.handleCameraIntent(context, intent)
            
            if (virtualCameraIntent != null) {
                // Launch virtual camera
                virtualCameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(virtualCameraIntent)
                Log.d(TAG, "Virtual camera launched successfully")
            } else {
                Log.e(TAG, "Failed to create virtual camera intent")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling camera intent: ${e.message}")
        }
    }
}
