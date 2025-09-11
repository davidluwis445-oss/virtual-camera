package com.app001.virtualcamera.video

import android.content.Context
import android.graphics.*
import android.media.Image
import android.util.Log
import android.util.Size
import com.app001.virtualcamera.VideoFileManager
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class VideoStreamManager(private val context: Context) {
    private val tag = "VideoStreamManager"
    private var videoFileManager: VideoFileManager? = null
    private var isVideoLoaded = false
    private var videoWidth = 0
    private var videoHeight = 0
    private var frameRate = 30
    private var currentFrameIndex = 0
    private var lastFrameTime = 0L
    
    // Video overlay properties
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

    fun processFrame(cameraImage: Image) {
        if (!isVideoLoaded || videoFileManager == null) return

        val currentTime = System.currentTimeMillis()
        val frameInterval = 1000 / frameRate

        // Check if it's time for the next video frame
        if (currentTime - lastFrameTime >= frameInterval) {
            try {
                val frameData = videoFileManager?.getFrame()
                if (frameData != null) {
                    // Create video overlay
                    createVideoOverlay(frameData)
                    
                    // Apply overlay to camera image
                    applyOverlayToCameraImage(cameraImage)
                    
                    lastFrameTime = currentTime
                    currentFrameIndex++
                }
            } catch (e: Exception) {
                Log.e(tag, "Error processing video frame: ${e.message}")
            }
        }
    }

    private fun createVideoOverlay(frameData: ByteArray) {
        try {
            // Convert frame data to bitmap
            val videoBitmap = convertFrameDataToBitmap(frameData)
            if (videoBitmap != null) {
                // Draw video frame on overlay canvas
                overlayCanvas?.drawBitmap(videoBitmap, 0f, 0f, paint)
                videoBitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error creating video overlay: ${e.message}")
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

    private fun applyOverlayToCameraImage(cameraImage: Image) {
        try {
            val planes = cameraImage.planes
            if (planes.isNotEmpty()) {
                val yPlane = planes[0]
                val yBuffer = yPlane.buffer
                
                // This is where we would inject the video overlay into the camera stream
                // For now, we'll simulate the injection
                injectVideoIntoYPlane(yBuffer)
                
                Log.d(tag, "Video overlay applied to camera frame")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error applying overlay to camera image: ${e.message}")
        }
    }

    private fun injectVideoIntoYPlane(yBuffer: ByteBuffer) {
        try {
            // Get video overlay data
            val overlayBitmap = overlayBitmap ?: return
            
            // Convert overlay bitmap to YUV format
            val yuvData = convertBitmapToYUV(overlayBitmap)
            
            // Inject into Y plane (simplified)
            if (yuvData.size <= yBuffer.remaining()) {
                yBuffer.put(yuvData, 0, minOf(yuvData.size, yBuffer.remaining()))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error injecting video into Y plane: ${e.message}")
        }
    }

    private fun convertBitmapToYUV(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val yuvSize = width * height * 3 / 2
        val yuv = ByteArray(yuvSize)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var yIndex = 0
        var uvIndex = width * height
        
        for (i in 0 until height) {
            for (j in 0 until width) {
                val pixel = pixels[i * width + j]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                // Convert RGB to YUV
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                
                yuv[yIndex++] = y.toByte()
                
                // Subsample UV
                if (i % 2 == 0 && j % 2 == 0) {
                    yuv[uvIndex++] = u.toByte()
                    yuv[uvIndex++] = v.toByte()
                }
            }
        }
        
        return yuv
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
        videoFileManager?.release()
        videoFileManager = null
        overlayBitmap?.recycle()
        overlayBitmap = null
        overlayCanvas = null
        isVideoLoaded = false
    }

    data class VideoInfo(
        val width: Int,
        val height: Int,
        val frameRate: Int,
        val currentFrame: Int
    )
}
