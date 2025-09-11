package com.app001.virtualcamera

import android.content.res.AssetManager
import android.util.Log
import java.nio.ByteBuffer

class VideoFileManager(private val assetManager: AssetManager) {
    private external fun initVideoProcessor(assetManager: AssetManager, videoPath: String): Long
    private external fun releaseVideoProcessor(processorPtr: Long)
    private external fun getNextFrame(processorPtr: Long): ByteArray
    private external fun getVideoWidth(processorPtr: Long): Int
    private external fun getVideoHeight(processorPtr: Long): Int
    private external fun getVideoFrameRate(processorPtr: Long): Int
    private external fun isVideoInitialized(processorPtr: Long): Boolean
    private external fun seekToTime(processorPtr: Long, timeMs: Long)

    private var processorPtr: Long = 0
    private val tag = "VideoFileManager"
    private var currentPosition: Long = 0

    init {
        System.loadLibrary("virtualcamera")
    }

    fun loadVideo(videoPath: String): Boolean {
        if (processorPtr != 0L) {
            releaseVideoProcessor(processorPtr)
        }

        processorPtr = initVideoProcessor(assetManager, videoPath)
        val initialized = isVideoInitialized(processorPtr)

        if (initialized) {
            Log.d(tag, "Video loaded successfully: $videoPath")
            Log.d(tag, "Video dimensions: ${getVideoWidth(processorPtr)}x${getVideoHeight(processorPtr)}")
            Log.d(tag, "Frame rate: ${getVideoFrameRate(processorPtr)}")
        } else {
            Log.e(tag, "Failed to load video: $videoPath")
        }

        return initialized
    }

    fun getFrame(): ByteArray? {
        if (processorPtr == 0L) {
            Log.e(tag, "Video processor not initialized")
            return null
        }

        return try {
            val frame = getNextFrame(processorPtr)
            currentPosition += (1000 / getVideoFrameRate(processorPtr)) // Advance time
            frame
        } catch (e: Exception) {
            Log.e(tag, "Error getting frame: ${e.message}")
            null
        }
    }

    fun seekTo(timeMs: Long) {
        if (processorPtr != 0L) {
            seekToTime(processorPtr, timeMs)
            currentPosition = timeMs
        }
    }

    fun getCurrentPosition(): Long = currentPosition

    fun getVideoDimensions(): Pair<Int, Int> {
        if (processorPtr == 0L) {
            return Pair(640, 480) // Default resolution
        }
        return Pair(getVideoWidth(processorPtr), getVideoHeight(processorPtr))
    }

    fun getFrameRate(): Int {
        if (processorPtr == 0L) {
            return 30 // Default frame rate
        }
        return getVideoFrameRate(processorPtr)
    }

    fun release() {
        if (processorPtr != 0L) {
            releaseVideoProcessor(processorPtr)
            processorPtr = 0
            currentPosition = 0
        }
    }
}

//package com.app001.virtualcamera
//
//import android.content.res.AssetManager
//import android.util.Log
//
//class VideoFileManager(private val assetManager: AssetManager) {
//    private external fun initVideoProcessor(assetManager: AssetManager, videoPath: String): Long
//    private external fun releaseVideoProcessor(processorPtr: Long)
//    private external fun getNextFrame(processorPtr: Long): ByteArray
//    private external fun getVideoWidth(processorPtr: Long): Int
//    private external fun getVideoHeight(processorPtr: Long): Int
//    private external fun getVideoFrameRate(processorPtr: Long): Int
//    private external fun isVideoInitialized(processorPtr: Long): Boolean
//
//    private var processorPtr: Long = 0
//    private val tag = "VideoFileManager"
//
//    init {
//        System.loadLibrary("virtualcamera")
//    }
//
//    fun loadVideo(videoPath: String): Boolean {
//        if (processorPtr != 0L) {
//            releaseVideoProcessor(processorPtr)
//        }
//
//        processorPtr = initVideoProcessor(assetManager, videoPath)
//        val initialized = isVideoInitialized(processorPtr)
//
//        if (initialized) {
//            Log.d(tag, "Video loaded successfully: $videoPath")
//            Log.d(tag, "Video dimensions: ${getVideoWidth(processorPtr)}x${getVideoHeight(processorPtr)}")
//            Log.d(tag, "Frame rate: ${getVideoFrameRate(processorPtr)}")
//        } else {
//            Log.e(tag, "Failed to load video: $videoPath")
//        }
//
//        return initialized
//    }
//
//    fun getFrame(): ByteArray? {
//        if (processorPtr == 0L) {
//            Log.e(tag, "Video processor not initialized")
//            return null
//        }
//
//        return try {
//            getNextFrame(processorPtr)
//        } catch (e: Exception) {
//            Log.e(tag, "Error getting frame: ${e.message}")
//            null
//        }
//    }
//
//    fun getVideoDimensions(): Pair<Int, Int> {
//        if (processorPtr == 0L) {
//            return Pair(0, 0)
//        }
//
//        return Pair(getVideoWidth(processorPtr), getVideoHeight(processorPtr))
//    }
//
//    fun getFrameRate(): Int {
//        if (processorPtr == 0L) {
//            return 0
//        }
//
//        return getVideoFrameRate(processorPtr)
//    }
//
//    fun release() {
//        if (processorPtr != 0L) {
//            releaseVideoProcessor(processorPtr)
//            processorPtr = 0
//        }
//    }
//}