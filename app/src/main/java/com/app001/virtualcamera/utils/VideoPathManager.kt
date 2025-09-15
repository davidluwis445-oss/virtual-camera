package com.app001.virtualcamera.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages the current video path for the virtual camera
 * This allows external apps to access the selected video
 */
object VideoPathManager {
    private const val PREFS_NAME = "virtual_camera_prefs"
    private const val KEY_VIDEO_PATH = "current_video_path"
    private const val TAG = "VideoPathManager"
    
    private var sharedPrefs: SharedPreferences? = null
    
    fun initialize(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "VideoPathManager initialized")
    }
    
    fun setCurrentVideoPath(videoPath: String?) {
        sharedPrefs?.edit()?.apply {
            if (videoPath != null) {
                putString(KEY_VIDEO_PATH, videoPath)
                Log.d(TAG, "Video path saved: $videoPath")
            } else {
                remove(KEY_VIDEO_PATH)
                Log.d(TAG, "Video path cleared")
            }
            apply()
        }
    }
    
    fun getCurrentVideoPath(): String? {
        val path = sharedPrefs?.getString(KEY_VIDEO_PATH, null)
        Log.d(TAG, "Video path retrieved: $path")
        return path
    }
    
    fun hasVideoPath(): Boolean {
        val hasPath = getCurrentVideoPath() != null
        Log.d(TAG, "Has video path: $hasPath")
        return hasPath
    }
}
