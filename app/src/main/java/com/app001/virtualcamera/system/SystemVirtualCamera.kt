package com.app001.virtualcamera.system

import android.content.Context
import android.util.Log
import java.io.DataOutputStream
import java.io.IOException

/**
 * Data class to represent the result of a root access request
 */
data class RootRequestResult(
    val success: Boolean,
    val message: String
)

class SystemVirtualCamera(private val context: Context) {
    private val tag = "SystemVirtualCamera"
    private var isRooted = false
    private var isVirtualCameraActive = false

    init {
        checkRootAccess()
    }

    private fun checkRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("id\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            outputStream.close()
            
            val exitCode = process.waitFor()
            isRooted = (exitCode == 0)
            
            if (isRooted) {
                Log.d(tag, "Device is rooted - system virtual camera can be installed")
            } else {
                Log.e(tag, "Device is not rooted - system virtual camera cannot be installed")
            }
            
            isRooted
        } catch (e: Exception) {
            Log.e(tag, "Error checking root access: ${e.message}")
            false
        }
    }

    fun installSystemVirtualCamera(): Boolean {
        if (!isRooted) {
            Log.e(tag, "Cannot install system virtual camera - device not rooted")
            return false
        }

        return try {
            // First, copy the native library to system and set up SELinux policies
            val commands = arrayOf(
                "su",
                "-c",
                "mount -o rw,remount /system",
                "su",
                "-c",
                "mkdir -p /system/lib64/",
                "su",
                "-c",
                "cp /data/app/*/com.app001.virtualcamera*/lib/arm64/libsystem_camera_hook.so /system/lib64/libvirtualcamera.so",
                "su",
                "-c",
                "chmod 644 /system/lib64/libvirtualcamera.so",
                "su",
                "-c",
                "chown root:root /system/lib64/libvirtualcamera.so",
                "su",
                "-c",
                "setsebool -P allow_camera_virtual 1",
                "su",
                "-c",
                "setprop persist.camera.virtual.enabled 1",
                "su",
                "-c",
                "setprop camera.hal.virtual 1",
                "su",
                "-c",
                "echo 'libvirtualcamera.so' >> /system/etc/public.libraries.txt",
                "su",
                "-c",
                "mount -o ro,remount /system"
            )

            val process = Runtime.getRuntime().exec(commands)
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                // Now install the native hooks
                val hookInstalled = installSystemHook()
                if (hookInstalled) {
                    isVirtualCameraActive = true
                    Log.d(tag, "System virtual camera installed successfully")
                    true
                } else {
                    Log.e(tag, "Failed to install system hooks")
                    false
                }
            } else {
                Log.e(tag, "Failed to install system virtual camera")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception installing system virtual camera: ${e.message}")
            false
        }
    }

    fun uninstallSystemVirtualCamera(): Boolean {
        if (!isRooted) {
            Log.e(tag, "Cannot uninstall system virtual camera - device not rooted")
            return false
        }

        return try {
            val commands = arrayOf(
                "su",
                "-c",
                "mount -o rw,remount /system",
                "su",
                "-c",
                "rm -f /system/lib64/libvirtualcamera.so",
                "su",
                "-c",
                "mount -o ro,remount /system"
            )

            val process = Runtime.getRuntime().exec(commands)
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                isVirtualCameraActive = false
                Log.d(tag, "System virtual camera uninstalled successfully")
                true
            } else {
                Log.e(tag, "Failed to uninstall system virtual camera")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception uninstalling system virtual camera: ${e.message}")
            false
        }
    }

    fun startVirtualCameraFeed(videoPath: String): Boolean {
        if (!isRooted) {
            Log.e(tag, "Cannot start virtual camera feed - device not rooted")
            return false
        }

        return try {
            // Load video in native code
            val videoLoaded = loadVideo(videoPath)
            if (!videoLoaded) {
                Log.e(tag, "Failed to load video: $videoPath")
                return false
            }
            
            // Start virtual camera
            startVirtualCamera()
            
            Log.d(tag, "Virtual camera feed started successfully with video: $videoPath")
            true
        } catch (e: Exception) {
            Log.e(tag, "Exception starting virtual camera feed: ${e.message}")
            false
        }
    }

    fun stopVirtualCameraFeed(): Boolean {
        if (!isRooted) {
            Log.e(tag, "Cannot stop virtual camera feed - device not rooted")
            return false
        }

        return try {
            // Stop virtual camera
            stopVirtualCamera()
            
            Log.d(tag, "Virtual camera feed stopped successfully")
            true
        } catch (e: Exception) {
            Log.e(tag, "Exception stopping virtual camera feed: ${e.message}")
            false
        }
    }

    fun isDeviceRooted(): Boolean = isRooted
    fun isVirtualCameraInstalled(): Boolean {
        return try {
            if (!isRooted) return false
            
            // Check if the system app file exists
            val result = executeRootCommand("ls -la /system/priv-app/VirtualCamera.apk")
            result.contains("VirtualCamera.apk")
        } catch (e: Exception) {
            Log.e(tag, "Exception checking virtual camera installation: ${e.message}")
            false
        }
    }
    
    /**
     * Execute a root command and return the output
     * This method will trigger a superuser permission request if root is available
     */
    fun executeRootCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = process.inputStream
            val errorStream = process.errorStream
            
            // Write the command
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            outputStream.close()
            
            // Read output
            val output = inputStream.bufferedReader().readText()
            val error = errorStream.bufferedReader().readText()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Log.d(tag, "Root command executed successfully: $command")
                output
            } else {
                Log.e(tag, "Root command failed with exit code $exitCode: $error")
                ""
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception executing root command '$command': ${e.message}")
            ""
        }
    }
    
    /**
     * Request superuser permissions by attempting to execute a simple root command
     * This will trigger the root manager to show a permission dialog
     */
    fun requestSuperuserPermissions(): Boolean {
        return try {
            val result = executeRootCommand("id")
            result.contains("uid=0") // Check if we got root user ID
        } catch (e: Exception) {
            Log.e(tag, "Exception requesting superuser permissions: ${e.message}")
            false
        }
    }
    
    /**
     * Programmatically request root access using multiple methods
     * This will actively trigger root manager permission dialogs
     */
    fun programmaticallyRequestRootAccess(): RootRequestResult {
        val methods = listOf(
            ::requestRootViaSuCommand,
            ::requestRootViaIdCommand,
            ::requestRootViaWhoamiCommand,
            ::requestRootViaEchoCommand
        )
        
        for ((index, method) in methods.withIndex()) {
            try {
                Log.d(tag, "Attempting root request method ${index + 1}")
                val result = method()
                if (result.success) {
                    Log.d(tag, "Root access granted via method ${index + 1}")
                    return RootRequestResult(true, "Root access granted via method ${index + 1}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Method ${index + 1} failed: ${e.message}")
            }
        }
        
        return RootRequestResult(false, "All root request methods failed")
    }
    
    /**
     * Request root via 'su' command
     */
    private fun requestRootViaSuCommand(): RootRequestResult {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = process.inputStream
            val errorStream = process.errorStream
            
            outputStream.writeBytes("echo 'Root access test'\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            outputStream.close()
            
            val output = inputStream.bufferedReader().readText()
            val error = errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0 && output.contains("Root access test")) {
                RootRequestResult(true, "SU command successful")
            } else {
                RootRequestResult(false, "SU command failed: $error")
            }
        } catch (e: Exception) {
            RootRequestResult(false, "SU command exception: ${e.message}")
        }
    }
    
    /**
     * Request root via 'id' command
     */
    private fun requestRootViaIdCommand(): RootRequestResult {
        return try {
            val result = executeRootCommand("id")
            if (result.contains("uid=0")) {
                RootRequestResult(true, "ID command successful - root access confirmed")
            } else {
                RootRequestResult(false, "ID command failed - no root access")
            }
        } catch (e: Exception) {
            RootRequestResult(false, "ID command exception: ${e.message}")
        }
    }
    
    /**
     * Request root via 'whoami' command
     */
    private fun requestRootViaWhoamiCommand(): RootRequestResult {
        return try {
            val result = executeRootCommand("whoami")
            if (result.contains("root")) {
                RootRequestResult(true, "Whoami command successful - running as root")
            } else {
                RootRequestResult(false, "Whoami command failed - not running as root")
            }
        } catch (e: Exception) {
            RootRequestResult(false, "Whoami command exception: ${e.message}")
        }
    }
    
    /**
     * Request root via 'echo' command
     */
    private fun requestRootViaEchoCommand(): RootRequestResult {
        return try {
            val testString = "RootTest${System.currentTimeMillis()}"
            val result = executeRootCommand("echo '$testString'")
            if (result.contains(testString)) {
                RootRequestResult(true, "Echo command successful - root access confirmed")
            } else {
                RootRequestResult(false, "Echo command failed - no root access")
            }
        } catch (e: Exception) {
            RootRequestResult(false, "Echo command exception: ${e.message}")
        }
    }
    
    /**
     * Force root permission request by attempting multiple root operations
     * This method is more aggressive in requesting root access
     */
    fun forceRootPermissionRequest(): Boolean {
        return try {
            Log.d(tag, "Starting force root permission request")
            
            // Try multiple root operations in sequence
            val operations = listOf(
                "id",
                "whoami", 
                "echo 'Root access test'",
                "ls /system",
                "cat /proc/version"
            )
            
            var successCount = 0
            for (operation in operations) {
                try {
                    val result = executeRootCommand(operation)
                    if (result.isNotEmpty()) {
                        successCount++
                        Log.d(tag, "Root operation successful: $operation")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Root operation failed: $operation - ${e.message}")
                }
            }
            
            val success = successCount > 0
            Log.d(tag, "Force root permission request completed. Success count: $successCount")
            success
        } catch (e: Exception) {
            Log.e(tag, "Exception in force root permission request: ${e.message}")
            false
        }
    }
    
    /**
     * Check if v4l2loopback is available on the device
     * This indicates if a custom kernel with v4l2loopback is installed
     */
    fun checkV4L2LoopbackAvailability(): Boolean {
        return try {
            val result = executeRootCommand("ls /dev/video*")
            val hasVideoDevices = result.contains("/dev/video")
            val hasLoopbackModule = executeRootCommand("lsmod | grep v4l2loopback").isNotEmpty()
            
            Log.d(tag, "V4L2Loopback check - Video devices: $hasVideoDevices, Module: $hasLoopbackModule")
            hasVideoDevices && hasLoopbackModule
        } catch (e: Exception) {
            Log.e(tag, "Exception checking v4l2loopback: ${e.message}")
            false
        }
    }
    
    /**
     * Install the app as a system app for mock camera functionality
     * This requires root access and will make the app a system app
     */
    fun installAsSystemApp(): Boolean {
        if (!isRooted) {
            Log.e(tag, "Cannot install as system app - device not rooted")
            return false
        }
        
        return try {
            Log.d(tag, "Installing app as system app...")
            
            val packageName = context.packageName
            val apkPath = context.applicationInfo.sourceDir
            
            // Create directory first
            executeRootCommand("mkdir -p /system/priv-app")
            
            val commands = arrayOf(
                "mount -o rw,remount /system",
                "cp '$apkPath' /system/priv-app/VirtualCamera.apk",
                "chmod 644 /system/priv-app/VirtualCamera.apk",
                "chown root:root /system/priv-app/VirtualCamera.apk",
                "mount -o ro,remount /system"
            )
            
            var successCount = 0
            for (command in commands) {
                try {
                    val result = executeRootCommand(command)
                    if (result.isNotEmpty() || command.contains("mount")) {
                        successCount++
                        Log.d(tag, "System app command successful: $command")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Command failed: $command - ${e.message}")
                }
            }
            
            // Check if the file was actually copied
            val checkResult = executeRootCommand("ls -la /system/priv-app/VirtualCamera.apk")
            val fileExists = checkResult.contains("VirtualCamera.apk")
            
            val success = successCount >= 3 && fileExists // At least 3 commands succeed and file exists
            Log.d(tag, "System app installation completed. Success count: $successCount, File exists: $fileExists")
            success
        } catch (e: Exception) {
            Log.e(tag, "Exception installing as system app: ${e.message}")
            false
        }
    }
    
    /**
     * Disable the default camera app to prevent conflicts
     * This helps ensure our virtual camera is used instead
     */
    fun disableDefaultCamera(): Boolean {
        if (!isRooted) {
            Log.e(tag, "Cannot disable default camera - device not rooted")
            return false
        }
        
        return try {
            Log.d(tag, "Disabling default camera app...")
            
            // Common camera package names across different devices
            val cameraPackages = listOf(
                "com.android.camera",
                "com.android.camera2",
                "com.google.android.GoogleCamera",
                "com.samsung.camera",
                "com.huawei.camera",
                "com.xiaomi.camera",
                "com.oneplus.camera"
            )
            
            var disabledCount = 0
            for (packageName in cameraPackages) {
                try {
                    val result = executeRootCommand("pm disable-user $packageName")
                    if (result.contains("Package $packageName new state: disabled-user")) {
                        disabledCount++
                        Log.d(tag, "Disabled camera package: $packageName")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to disable $packageName: ${e.message}")
                }
            }
            
            val success = disabledCount > 0
            Log.d(tag, "Camera disabling completed. Disabled count: $disabledCount")
            success
        } catch (e: Exception) {
            Log.e(tag, "Exception disabling default camera: ${e.message}")
            false
        }
    }
    
    /**
     * Set up v4l2loopback virtual camera device
     * This creates a virtual camera device that can be used by other apps
     */
    fun setupV4L2LoopbackDevice(): Boolean {
        if (!isRooted) {
            Log.e(tag, "Cannot setup v4l2loopback - device not rooted")
            return false
        }
        
        return try {
            Log.d(tag, "Setting up v4l2loopback device...")
            
            val commands = arrayOf(
                "modprobe v4l2loopback",
                "lsmod | grep v4l2loopback",
                "ls /dev/video*",
                "chmod 666 /dev/video*"
            )
            
            var successCount = 0
            for (command in commands) {
                val result = executeRootCommand(command)
                if (result.isNotEmpty()) {
                    successCount++
                    Log.d(tag, "V4L2Loopback command successful: $command")
                }
            }
            
            val success = successCount >= 2 // At least 2 commands should succeed
            Log.d(tag, "V4L2Loopback setup completed. Success count: $successCount")
            success
        } catch (e: Exception) {
            Log.e(tag, "Exception setting up v4l2loopback: ${e.message}")
            false
        }
    }
    
    /**
     * Stream video to v4l2loopback device using ffmpeg
     * This feeds the selected video to the virtual camera device
     */
    fun streamVideoToV4L2Loopback(videoPath: String, devicePath: String = "/dev/video0"): Boolean {
        if (!isRooted) {
            Log.e(tag, "Cannot stream video - device not rooted")
            return false
        }
        
        return try {
            Log.d(tag, "Streaming video to v4l2loopback device: $devicePath")
            
            // Check if ffmpeg is available
            val ffmpegCheck = executeRootCommand("which ffmpeg")
            if (ffmpegCheck.isEmpty()) {
                Log.e(tag, "FFmpeg not found - cannot stream video")
                return false
            }
            
            // Stream video to v4l2loopback device
            val ffmpegCommand = "ffmpeg -re -i '$videoPath' -f v4l2 '$devicePath' -y"
            val result = executeRootCommand(ffmpegCommand)
            
            val success = result.isNotEmpty()
            Log.d(tag, "Video streaming completed. Success: $success")
            success
        } catch (e: Exception) {
            Log.e(tag, "Exception streaming video to v4l2loopback: ${e.message}")
            false
        }
    }
    
    /**
     * Get available video devices for v4l2loopback
     * Returns list of available video devices
     */
    fun getAvailableVideoDevices(): List<String> {
        return try {
            val result = executeRootCommand("ls /dev/video*")
            val devices = result.split("\n")
                .filter { it.contains("/dev/video") }
                .map { it.trim() }
            
            Log.d(tag, "Available video devices: $devices")
            devices
        } catch (e: Exception) {
            Log.e(tag, "Exception getting video devices: ${e.message}")
            emptyList()
        }
    }

    // Native JNI functions
    private external fun installSystemHook(): Boolean
    private external fun uninstallSystemHook()
    private external fun loadVideo(videoPath: String): Boolean
    private external fun startVirtualCamera()
    private external fun stopVirtualCamera()
    private external fun isHookInstalled(): Boolean

    companion object {
        init {
            System.loadLibrary("system_camera_hook")
        }
    }
}
