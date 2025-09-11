package com.app001.virtualcamera.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.app001.virtualcamera.video.VideoStreamManager

class VirtualCamera2(private val context: Context, private val videoStreamManager: VideoStreamManager) {
    private val tag = "VirtualCamera2"
    private var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var isActive = false
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(tag, "Camera opened")
            cameraDevice = camera
            createCameraCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(tag, "Camera disconnected")
            camera.close()
            cameraDevice = null
            isActive = false
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(tag, "Camera error: $error")
            camera.close()
            cameraDevice = null
            isActive = false
        }
    }

    fun start() {
        if (isActive) return
        try {
            startBackgroundThread()
            
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isEmpty()) {
                Log.e(tag, "No camera devices found")
                return
            }

            // Use the first available camera (usually back camera)
            val cameraId = cameraIds[0]
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // Get optimal preview size
            val previewSize = streamConfigMap?.getOutputSizes(SurfaceTexture::class.java)?.maxByOrNull { it.width * it.height }
                ?: Size(1920, 1080)

            // Create ImageReader for virtual camera output
            imageReader = ImageReader.newInstance(
                previewSize.width,
                previewSize.height,
                android.graphics.ImageFormat.YUV_420_888,
                2
            )

            // Set up image reader callback
            imageReader?.setOnImageAvailableListener({ reader ->
                processCameraFrame(reader)
            }, backgroundHandler)

            // Open camera
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
            isActive = true
            Log.d(tag, "Virtual camera started")
        } catch (e: CameraAccessException) {
            Log.e(tag, "Camera access exception: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(tag, "Camera permission denied: ${e.message}")
        }
    }

    fun stop() {
        if (!isActive) return
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        stopBackgroundThread()
        isActive = false
        Log.d(tag, "Virtual camera stopped")
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(tag, "Interrupted while stopping background thread", e)
        }
    }

    private fun createCameraCaptureSession() {
        val cameraDevice = cameraDevice ?: return
        val imageReader = imageReader ?: return

        try {
            val surface = imageReader.surface
            cameraDevice.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(tag, "CameraCaptureSession configured")
                        captureSession = session
                        try {
                            val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            captureRequestBuilder.addTarget(surface)
                            session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            Log.e(tag, "Failed to set repeating request: ${e.message}")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(tag, "CameraCaptureSession configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(tag, "Failed to create capture session: ${e.message}")
        }
    }

    private fun processCameraFrame(reader: ImageReader) {
        val image = reader.acquireLatestImage() ?: return
        
        try {
            // In a real implementation, you would inject the video frame here
            // by replacing the image data with your video frame
            Log.d(tag, "Processing camera frame")
            
            // For now, we'll just log that we're processing frames
            // The actual frame injection would require more complex implementation
        } catch (e: Exception) {
            Log.e(tag, "Error processing camera frame: ${e.message}")
        } finally {
            image.close()
        }
    }

    fun release() {
        stop()
    }
}
