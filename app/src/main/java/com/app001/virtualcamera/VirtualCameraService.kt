package com.app001.virtualcamera

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class VirtualCameraService : Service() {
    private lateinit var cameraInterceptor: CameraInterceptor

    companion object {
        const val TAG = "VirtualCameraService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service creating")
        cameraInterceptor = CameraInterceptor(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service starting")
        try {
            cameraInterceptor.interceptSystemCamera()
        } catch (e: SecurityException) {
            Log.e(TAG, "Root access required: ${e.message}")
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroying")
        cameraInterceptor.restoreSystemCamera()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}