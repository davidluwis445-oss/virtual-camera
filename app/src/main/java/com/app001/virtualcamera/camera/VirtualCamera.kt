package com.app001.virtualcamera.camera

import android.hardware.camera2.*
import android.media.ImageReader
import android.util.Log
import android.util.Size
import android.view.Surface
import com.app001.virtualcamera.video.VideoStreamManager
import kotlinx.coroutines.*
import java.util.concurrent.Executors

class VirtualCamera(
    private val cameraManager: CameraManager,
    private val videoStreamManager: VideoStreamManager
) {
    private val tag = "VirtualCamera"
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var isActive = false
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val cameraScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(tag, "Camera opened")
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(tag, "Camera disconnected")
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(tag, "Camera error: $error")
            camera.close()
            cameraDevice = null
        }
    }

    fun start() {
        if (isActive) return
        
        try {
            // Get available cameras
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isEmpty()) {
                Log.e(tag, "No cameras available")
                return
            }

            // Use the first available camera (usually back camera)
            val cameraId = cameraIds[0]
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            
            // Get optimal preview size
            val previewSize = streamConfigMap?.getOutputSizes(android.graphics.SurfaceTexture::class.java)?.maxByOrNull { it.width * it.height }
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
            }, null)

            // Open camera
            cameraManager.openCamera(cameraId, stateCallback, null)
            isActive = true
            
            Log.d(tag, "Virtual camera started with size: ${previewSize.width}x${previewSize.height}")
            
        } catch (e: SecurityException) {
            Log.e(tag, "Camera permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start virtual camera: ${e.message}")
        }
    }

    fun stop() {
        if (!isActive) return
        
        isActive = false
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
        
        captureSession = null
        cameraDevice = null
        imageReader = null
        
        Log.d(tag, "Virtual camera stopped")
    }

    private fun createCaptureSession() {
        val camera = cameraDevice ?: return
        val imageReader = imageReader ?: return

        try {
            val surfaces = listOf(imageReader.surface)
            
            camera.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(tag, "Capture session configured")
                        captureSession = session
                        startRepeatingCapture()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(tag, "Capture session configuration failed")
                    }
                },
                null
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to create capture session: ${e.message}")
        }
    }

    private fun startRepeatingCapture() {
        val session = captureSession ?: return
        val camera = cameraDevice ?: return

        try {
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(imageReader!!.surface)
            
            // Set up capture request
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

            session.setRepeatingRequest(
                captureRequestBuilder.build(),
                null,
                null
            )
            
            Log.d(tag, "Repeating capture started")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start repeating capture: ${e.message}")
        }
    }

    private fun processCameraFrame(reader: ImageReader) {
        try {
            val image = reader.acquireLatestImage()
            if (image != null) {
                // Process the camera frame and inject video overlay
                videoStreamManager.processFrame(image)
                image.close()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing camera frame: ${e.message}")
        }
    }

    fun isRunning(): Boolean = isActive
}
