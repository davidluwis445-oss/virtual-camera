package com.app001.virtualcamera.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.app001.virtualcamera.video.VideoOverlayManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VirtualCameraPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private val tag = "VirtualCameraPreview"
    private var videoOverlayManager: VideoOverlayManager? = null
    private var isPreviewActive = false
    private var currentVideoPath: String? = null
    private var canvas: Canvas? = null
    private var paint: Paint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
    }

    init {
        holder.addCallback(this)
        videoOverlayManager = VideoOverlayManager(context)
    }

    fun startPreview() {
        if (isPreviewActive) return
        
        isPreviewActive = true
        Log.d(tag, "Camera preview started")
        
        // Start drawing loop
        startDrawingLoop()
    }

    fun stopPreview() {
        if (!isPreviewActive) return
        
        isPreviewActive = false
        Log.d(tag, "Camera preview stopped")
    }

    fun loadVideo(videoPath: String) {
        currentVideoPath = videoPath
        videoOverlayManager?.loadVideo(videoPath)
        Log.d(tag, "Video loaded: $videoPath")
    }

    fun unloadVideo() {
        currentVideoPath = null
        videoOverlayManager?.unloadVideo()
        Log.d(tag, "Video unloaded")
    }

    private fun startDrawingLoop() {
        Thread {
            while (isPreviewActive) {
                try {
                    val canvas = holder.lockCanvas()
                    if (canvas != null) {
                        drawPreview(canvas)
                        holder.unlockCanvasAndPost(canvas)
                    }
                    Thread.sleep(33) // ~30 FPS
                } catch (e: Exception) {
                    Log.e(tag, "Error in drawing loop: ${e.message}")
                }
            }
        }.start()
    }

    private fun drawPreview(canvas: Canvas) {
        // Clear canvas
        canvas.drawColor(Color.BLACK)
        
        // Draw camera preview simulation
        drawCameraSimulation(canvas)
        
        // Draw video overlay if active
        if (currentVideoPath != null) {
            drawVideoOverlay(canvas)
        }
    }

    private fun drawCameraSimulation(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        
        // Draw a simple camera preview simulation
        paint.color = Color.GRAY
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Draw some test pattern
        paint.color = Color.WHITE
        paint.textSize = 48f
        canvas.drawText("Camera Preview", 50f, height / 2f, paint)
        
        if (isPreviewActive) {
            paint.color = Color.GREEN
            canvas.drawCircle(width - 50f, 50f, 20f, paint)
        }
    }

    private fun drawVideoOverlay(canvas: Canvas) {
        // Draw video overlay simulation
        paint.color = Color.RED
        paint.alpha = 128
        canvas.drawRect(100f, 100f, 300f, 200f, paint)
        
        paint.color = Color.WHITE
        paint.alpha = 255
        paint.textSize = 24f
        canvas.drawText("Video Overlay", 120f, 150f, paint)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(tag, "Surface created")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(tag, "Surface changed: ${width}x${height}")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(tag, "Surface destroyed")
        stopPreview()
    }

    fun release() {
        stopPreview()
        videoOverlayManager?.release()
    }
}
