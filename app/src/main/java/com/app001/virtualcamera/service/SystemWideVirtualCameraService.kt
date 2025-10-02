package com.app001.virtualcamera.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.app001.virtualcamera.camera.RealCameraHook
import com.app001.virtualcamera.R

/**
 * System-Wide Virtual Camera Service
 * This service runs in the background and provides virtual camera to ALL apps
 */
class SystemWideVirtualCameraService : Service() {
    
    companion object {
        private const val TAG = "SystemWideVirtualCameraService"
        const val ACTION_START_SYSTEM_CAMERA = "com.app001.virtualcamera.START_SYSTEM_CAMERA"
        const val ACTION_STOP_SYSTEM_CAMERA = "com.app001.virtualcamera.STOP_SYSTEM_CAMERA"
        const val EXTRA_VIDEO_PATH = "video_path"
        
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "system_virtual_camera_channel"
    }
    
    private val realCameraHook = RealCameraHook.instance
    private var isServiceActive = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "System-wide virtual camera service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SYSTEM_CAMERA -> {
                val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
                startSystemWideVirtualCamera(videoPath)
            }
            ACTION_STOP_SYSTEM_CAMERA -> {
                stopSystemWideVirtualCamera()
            }
        }
        
        return START_STICKY // Keep service running
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun startSystemWideVirtualCamera(videoPath: String?) {
        if (isServiceActive) {
            Log.w(TAG, "System-wide virtual camera already active")
            return
        }
        
        Log.d(TAG, "Starting system-wide virtual camera with video: $videoPath")
        
        try {
            // Start foreground service with notification
            val notification = createServiceNotification(
                "🎥 Virtual Camera Active",
                "All camera apps will see your video instead of real camera"
            )
            startForeground(NOTIFICATION_ID, notification)
            
            // Enable real camera hooking
            val success = if (videoPath != null) {
                realCameraHook.enableSystemWideVirtualCamera(this, videoPath)
            } else {
                realCameraHook.installRealCameraHooks()
            }
            
            if (success) {
                isServiceActive = true
                Log.d(TAG, "✅ System-wide virtual camera started successfully!")
                Log.d(TAG, "🎯 TikTok, Instagram, Telegram will now see virtual camera!")
                
                // Update notification
                val activeNotification = createServiceNotification(
                    "✅ Virtual Camera Active",
                    "🎯 ALL camera apps now see your video! Open TikTok/Instagram to test."
                )
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, activeNotification)
                
            } else {
                Log.e(TAG, "❌ Failed to start system-wide virtual camera")
                stopSelf()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting system-wide virtual camera: ${e.message}")
            stopSelf()
        }
    }
    
    private fun stopSystemWideVirtualCamera() {
        if (!isServiceActive) {
            Log.w(TAG, "System-wide virtual camera not active")
            return
        }
        
        Log.d(TAG, "Stopping system-wide virtual camera")
        
        try {
            // Disable real camera hooking
            realCameraHook.disableSystemWideVirtualCamera()
            
            isServiceActive = false
            
            Log.d(TAG, "System-wide virtual camera stopped")
            
            // Stop foreground service
            stopForeground(true)
            stopSelf()
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping system-wide virtual camera: ${e.message}")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Virtual Camera",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "System-wide virtual camera service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createServiceNotification(title: String, content: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "System-wide virtual camera service destroyed")
        
        if (isServiceActive) {
            realCameraHook.disableSystemWideVirtualCamera()
        }
    }
}
