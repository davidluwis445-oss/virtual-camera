# Virtual Camera with Video Preview

This Android application provides a virtual camera preview with video overlay functionality. It allows you to overlay video content on top of the camera feed, creating a virtual camera effect.

## Features

- **Camera Preview**: Real-time camera preview using CameraX
- **Video Overlay**: Overlay video content on camera feed
- **Permission Management**: Proper camera and microphone permission handling
- **Modern UI**: Built with Jetpack Compose
- **Native Performance**: C++ video processing for better performance

## How It Works

1. **Camera Access**: The app requests camera and microphone permissions
2. **Video Loading**: Loads video files from the assets folder
3. **Frame Processing**: Processes video frames using native C++ code
4. **Overlay Rendering**: Overlays video frames on the camera preview
5. **Real-time Display**: Shows the combined camera + video feed

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 or higher
- NDK (Native Development Kit)
- A physical Android device (camera functionality doesn't work on emulator)

### Installation

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd VirtualCamera
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the VirtualCamera folder

3. **Build the project**:
   - Sync the project with Gradle files
   - Build the project (Build → Make Project)

4. **Install on device**:
   - Connect your Android device via USB
   - Enable USB debugging
   - Run the app (Run → Run 'app')

### Adding Video Files

1. Place your video files in `app/src/main/assets/`
2. Supported formats: MP4, AVI, MOV
3. Recommended resolution: 640x480 or 1280x720
4. Recommended frame rate: 30 FPS

## Usage

1. **Launch the app** on your Android device
2. **Grant permissions** when prompted (Camera and Microphone)
3. **Start the camera** by tapping the camera icon
4. **Load a video** by tapping the play button
5. **View the overlay** - you'll see the video overlaid on the camera feed

## Controls

- **Camera Toggle**: Start/stop the camera preview
- **Video Playback**: Play/pause video overlay
- **Settings**: Access app settings (future feature)
- **Record**: Record the combined feed (future feature)
- **Video Library**: Select different videos (future feature)

## Technical Details

### Architecture

- **UI Layer**: Jetpack Compose for modern UI
- **Camera Layer**: CameraX for camera functionality
- **Video Layer**: Custom video processing with C++
- **Overlay Layer**: Canvas-based video overlay system

### Key Components

- `MainActivity`: Main activity with permission handling
- `VirtualCameraViewModel`: State management
- `CameraPreviewScreen`: UI components
- `VirtualCameraPreview`: Camera preview implementation
- `VideoOverlayManager`: Video processing and overlay
- `VideoFileManager`: Native video file handling

### Native Code

The app includes C++ code for efficient video processing:
- `video_processor.cpp`: Video frame extraction
- `native-lib.cpp`: JNI interface
- `CMakeLists.txt`: Build configuration

## Troubleshooting

### Common Issues

1. **Camera not working**:
   - Ensure you're using a physical device
   - Check camera permissions
   - Restart the app

2. **Video not loading**:
   - Check if video file exists in assets folder
   - Verify video format is supported
   - Check logcat for error messages

3. **Build errors**:
   - Clean and rebuild the project
   - Check NDK installation
   - Verify Android SDK version

### Debugging

Enable debug logging to see detailed information:
```bash
adb logcat | grep -E "(VirtualCamera|VideoProcessor|CameraInterceptor)"
```

## Future Enhancements

- [ ] Real video file decoding (FFmpeg integration)
- [ ] Multiple video format support
- [ ] Video recording functionality
- [ ] Settings screen for configuration
- [ ] Video library management
- [ ] Real-time video effects
- [ ] Network streaming support

## License

This project is for educational purposes. Please ensure you have proper permissions for any video content you use.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review the logcat output
3. Create an issue with detailed information
4. Include device information and Android version
