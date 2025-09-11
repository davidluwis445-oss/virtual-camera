package com.app001.virtualcamera.video

import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageProxy
import com.app001.virtualcamera.VideoFileManager
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class VideoOverlayManager(private val context: Context) {
    private val tag = "VideoOverlayManager"
    private var videoFileManager: VideoFileManager? = null
    private var isVideoLoaded = false
    private var videoWidth = 0
    private var videoHeight = 0
    private var frameRate = 30
    private var currentFrameIndex = 0
    private var lastFrameTime = 0L
    
    // Canvas and bitmap for video overlay
    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null
    private var paint: Paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    fun loadVideo(videoPath: String) {
        try {
            videoFileManager = VideoFileManager(context.assets)
            val success = videoFileManager?.loadVideo(videoPath) ?: false
            
            if (success) {
                val dimensions = videoFileManager?.getVideoDimensions() ?: Pair(640, 480)
                videoWidth = dimensions.first
                videoHeight = dimensions.second
                frameRate = videoFileManager?.getFrameRate() ?: 30
                
                // Create overlay bitmap
                overlayBitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
                overlayCanvas = Canvas(overlayBitmap!!)
                
                isVideoLoaded = true
                currentFrameIndex = 0
                lastFrameTime = System.currentTimeMillis()
                
                Log.d(tag, "Video loaded successfully: ${videoWidth}x${videoHeight}, ${frameRate}fps")
            } else {
                Log.e(tag, "Failed to load video: $videoPath")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading video: ${e.message}")
        }
    }

    fun unloadVideo() {
        videoFileManager?.release()
        videoFileManager = null
        overlayBitmap?.recycle()
        overlayBitmap = null
        overlayCanvas = null
        isVideoLoaded = false
        Log.d(tag, "Video unloaded")
    }

    fun processFrame(imageProxy: ImageProxy) {
        if (!isVideoLoaded || videoFileManager == null) return

        val currentTime = System.currentTimeMillis()
        val frameInterval = 1000 / frameRate // milliseconds per frame

        // Check if it's time for the next frame
        if (currentTime - lastFrameTime >= frameInterval) {
            try {
                val frameData = videoFileManager?.getFrame()
                if (frameData != null) {
                    // Convert frame data to bitmap and overlay on camera feed
                    overlayVideoFrame(frameData, imageProxy)
                    lastFrameTime = currentTime
                    currentFrameIndex++
                }
            } catch (e: Exception) {
                Log.e(tag, "Error processing video frame: ${e.message}")
            }
        }
    }

    private fun overlayVideoFrame(frameData: ByteArray, imageProxy: ImageProxy) {
        try {
            // Convert frame data to bitmap
            val videoBitmap = convertFrameDataToBitmap(frameData)
            if (videoBitmap != null) {
                // Draw video frame on overlay canvas
                overlayCanvas?.drawBitmap(videoBitmap, 0f, 0f, paint)
                
                // Apply overlay to camera image
                applyOverlayToImage(imageProxy)
                
                videoBitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error overlaying video frame: ${e.message}")
        }
    }

    private fun convertFrameDataToBitmap(frameData: ByteArray): Bitmap? {
        return try {
            // Assuming frameData is in RGB format
            val bitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.RGB_565)
            val pixels = IntArray(videoWidth * videoHeight)
            
            // Convert RGB bytes to pixel array
            for (i in 0 until minOf(frameData.size / 3, pixels.size)) {
                val r = frameData[i * 3].toInt() and 0xFF
                val g = frameData[i * 3 + 1].toInt() and 0xFF
                val b = frameData[i * 3 + 2].toInt() and 0xFF
                pixels[i] = Color.rgb(r, g, b)
            }
            
            bitmap.setPixels(pixels, 0, videoWidth, 0, 0, videoWidth, videoHeight)
            bitmap
        } catch (e: Exception) {
            Log.e(tag, "Error converting frame data to bitmap: ${e.message}")
            null
        }
    }

    private fun applyOverlayToImage(imageProxy: ImageProxy) {
        try {
            val planes = imageProxy.planes
            if (planes.isNotEmpty()) {
                val yPlane = planes[0]
                val yBuffer = yPlane.buffer
                
                // This is a simplified overlay - in a real implementation,
                // you would properly blend the video overlay with the camera feed
                // For now, we'll just log that we're applying the overlay
                Log.d(tag, "Applying video overlay to camera frame")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error applying overlay to image: ${e.message}")
        }
    }

    fun getVideoInfo(): VideoInfo? {
        return if (isVideoLoaded) {
            VideoInfo(
                width = videoWidth,
                height = videoHeight,
                frameRate = frameRate,
                currentFrame = currentFrameIndex
            )
        } else {
            null
        }
    }

    fun seekToFrame(frameIndex: Int) {
        if (isVideoLoaded && videoFileManager != null) {
            val timeMs = (frameIndex * 1000L) / frameRate
            videoFileManager?.seekTo(timeMs)
            currentFrameIndex = frameIndex
            Log.d(tag, "Seeked to frame: $frameIndex")
        }
    }

    fun release() {
        unloadVideo()
    }

    data class VideoInfo(
        val width: Int,
        val height: Int,
        val frameRate: Int,
        val currentFrame: Int
    )
}
