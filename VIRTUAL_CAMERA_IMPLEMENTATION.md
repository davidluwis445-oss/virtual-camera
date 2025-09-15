# Virtual Camera Implementation

This document describes the complete virtual camera implementation that can replace the system camera app on Android devices.

## Overview

The virtual camera system consists of several key components that work together to provide a seamless camera replacement experience:

1. **Camera Provider Service** - Registers virtual cameras with the Android Camera Service
2. **Virtual Camera Device** - Implements the Camera2 API for virtual camera functionality
3. **Video Processor** - Handles video frame processing and format conversion using OpenCV
4. **System Camera Replacer** - Hooks into the system to replace default camera apps
5. **Native C++ Implementation** - High-performance video processing using JNI

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Android System                           │
├─────────────────────────────────────────────────────────────┤
│  Camera Service  │  Camera Manager  │  Camera Apps         │
├─────────────────────────────────────────────────────────────┤
│              Virtual Camera Provider Service                │
├─────────────────────────────────────────────────────────────┤
│  Virtual Camera Device  │  Video Processor  │  System Hooks │
├─────────────────────────────────────────────────────────────┤
│              Native C++ Video Processing                    │
│              (OpenCV + JNI)                                │
└─────────────────────────────────────────────────────────────┘
```

## Key Components

### 1. VirtualCameraProvider.kt
- Implements camera characteristics and device management
- Provides virtual camera enumeration
- Handles camera device creation and configuration

### 2. VirtualCameraDevice.kt
- Implements Camera2 API for virtual camera devices
- Manages capture sessions and requests
- Handles frame processing and output

### 3. VideoProcessor.kt
- Java interface to native video processing
- Handles video loading and playback
- Manages frame callbacks and surface rendering

### 4. SystemCameraReplacer.kt
- Hooks into system camera management
- Replaces default camera app intents
- Manages camera ID enumeration

### 5. Native C++ Implementation
- `video_processor.h/cpp` - OpenCV-based video processing
- `CMakeLists.txt` - Build configuration with OpenCV integration
- JNI bridge for Java-C++ communication

## Features

### ✅ Implemented Features
- **System Camera Replacement** - Replaces default camera app
- **Video Frame Processing** - Real-time video processing with OpenCV
- **Multiple Format Support** - YUV420, NV21, JPEG output formats
- **Surface Rendering** - Direct surface output for camera preview
- **Loop Playback** - Continuous video loop playback
- **Permission Handling** - Proper camera permission management
- **Error Handling** - Comprehensive error handling and logging

### 🔧 Technical Features
- **Camera2 API Integration** - Full Camera2 API implementation
- **JNI Native Bridge** - High-performance C++ video processing
- **OpenCV Integration** - Advanced image processing capabilities
- **Surface Texture Support** - Hardware-accelerated rendering
- **Thread Management** - Proper background thread handling
- **Memory Management** - Efficient resource management

## Installation & Setup

### Prerequisites
1. Android Studio with NDK support
2. OpenCV for Android SDK
3. Android device with Camera2 API support
4. Root access (for system-wide replacement)

### Build Configuration

1. **Add OpenCV to your project:**
   ```gradle
   // In app/build.gradle
   android {
       sourceSets {
           main {
               jniLibs.srcDirs = ['../opencv/sdk/native/libs']
           }
       }
   }
   ```

2. **Update CMakeLists.txt:**
   ```cmake
   # Find OpenCV
   find_path(OpenCV_INCLUDE_DIRS
       NAMES opencv2/opencv.hpp
       PATHS ${CMAKE_SOURCE_DIR}/../opencv/sdk/native/jni/include
   )
   
   find_library(OpenCV_LIBS
       NAMES opencv_java4
       PATHS ${CMAKE_SOURCE_DIR}/../opencv/sdk/native/libs/${ANDROID_ABI}
   )
   ```

3. **Add required permissions:**
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
   <uses-permission android:name="android.permission.WRITE_SETTINGS" />
   <uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS" />
   ```

## Usage

### Basic Usage

1. **Initialize the virtual camera:**
   ```kotlin
   val virtualCameraProvider = VirtualCameraProvider(context)
   val systemCameraReplacer = SystemCameraReplacer(context)
   val videoProcessor = VideoProcessor(context)
   ```

2. **Load a video file:**
   ```kotlin
   videoProcessor.loadVideo("/path/to/video.mp4")
   ```

3. **Start virtual camera:**
   ```kotlin
   systemCameraReplacer.replaceSystemCamera()
   videoProcessor.startProcessing()
   ```

4. **Set output surface:**
   ```kotlin
   videoProcessor.setOutputSurface(surfaceView.holder.surface)
   ```

### Advanced Usage

1. **Custom video processing:**
   ```kotlin
   videoProcessor.setFrameCallback { frame, userData ->
       // Process frame here
       // frame is OpenCV Mat object
   }
   ```

2. **Camera characteristics:**
   ```kotlin
   val characteristics = virtualCameraProvider.getCameraCharacteristics()
   val supportedSizes = virtualCameraProvider.getSupportedSizes(ImageFormat.YUV_420_888)
   ```

## System Integration

### Camera Provider Service
The virtual camera registers itself as a camera provider service:

```xml
<service
    android:name=".camera.VirtualCameraProviderService"
    android:enabled="true"
    android:exported="true"
    android:process=":cameraprovider">
    <intent-filter>
        <action android:name="android.hardware.camera.provider" />
    </intent-filter>
</service>
```

### Intent Filters
The app registers to handle camera intents:

```xml
<intent-filter>
    <action android:name="android.media.action.IMAGE_CAPTURE" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

## Performance Considerations

### Memory Management
- Uses efficient OpenCV Mat objects for frame processing
- Implements proper resource cleanup in onDestroy()
- Uses background threads for video processing

### Frame Rate Control
- Configurable frame rate (default 30 FPS)
- Adaptive frame rate based on video source
- Efficient frame skipping for performance

### Surface Rendering
- Hardware-accelerated surface rendering
- Proper surface format conversion
- Optimized memory copying

## Troubleshooting

### Common Issues

1. **Camera permission denied:**
   - Ensure CAMERA permission is granted
   - Check runtime permission handling

2. **Video not loading:**
   - Verify video file path and format
   - Check OpenCV library integration
   - Ensure proper file permissions

3. **System camera not replaced:**
   - Requires root access for system-wide replacement
   - Check camera provider service registration
   - Verify intent filter configuration

4. **Performance issues:**
   - Reduce video resolution
   - Lower frame rate
   - Check memory usage

### Debug Logging
Enable debug logging to troubleshoot issues:

```kotlin
Log.d("VirtualCamera", "Debug message")
```

## Limitations

1. **Root Access Required** - System-wide camera replacement requires root
2. **OpenCV Dependency** - Requires OpenCV for Android SDK
3. **Camera2 API** - Requires Android 5.0+ (API 21+)
4. **Performance** - Video processing may impact performance on older devices

## Future Enhancements

1. **Real-time Effects** - Add filters and effects
2. **Multiple Video Sources** - Support for multiple video inputs
3. **Streaming Support** - Real-time video streaming
4. **Advanced Processing** - AI-based video enhancement
5. **Cloud Integration** - Cloud-based video processing

## Security Considerations

1. **Permission Management** - Proper permission handling
2. **Data Privacy** - Secure video data handling
3. **System Integrity** - Safe system modification
4. **Access Control** - Proper access control mechanisms

## Conclusion

This virtual camera implementation provides a complete solution for replacing the system camera app with custom video content. The modular architecture allows for easy customization and extension while maintaining high performance and reliability.

For questions or support, please refer to the project documentation or contact the development team.

