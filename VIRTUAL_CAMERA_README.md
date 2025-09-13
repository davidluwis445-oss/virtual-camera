# Virtual Camera - System Virtual Camera for Android

## 🎥 **Virtual Camera Functionality Designed for Android Devices**

Perfect for various applications where a fake camera feed is required.

## ✨ **Features**

- **Virtual Camera**: Simulate a virtual camera feed on your Android device
- **Works with All Apps**: Use the fake camera in any app that accesses the camera, from live streaming platforms to video conferencing, and even online sales apps
- **Requires Root**: This application requires your device to be rooted in order to function properly
- **Android Compatibility**: Designed specifically for Android devices

## 🔧 **Requirements**

- **Rooted Android Device**: Your device must be rooted to install system-level virtual camera
- **Android 7.0+**: Compatible with Android 7.0 and above
- **Camera Permission**: App requires camera permissions to function

## 📱 **How to Use**

### 1. **Ensure your device is rooted**
- Use tools like Magisk, SuperSU, or KingRoot to root your device
- Verify root access is working properly

### 2. **Install the Virtual Camera app on your Android device**
```bash
# Build the app
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### 3. **Set up the virtual camera feed according to your preferences**
- Open the Virtual Camera app
- Check that your device is rooted (green status)
- Enter the path to your video file (default: `sample_video.mp4`)
- Tap "Install System Camera" to install the system-level virtual camera

### 4. **Select the Virtual Camera virtual camera in any app where you want to use a fake camera feed**
- Tap "Start Virtual Camera" to begin the virtual camera feed
- Open any camera app (Instagram, Snapchat, Camera, etc.)
- The app will now show your video instead of the real camera

### 5. **Enjoy using a virtual camera for live streaming, online sales, and more**
- Works with all camera apps
- Real-time video processing
- System-wide camera replacement

## 🎯 **Supported Apps**

The virtual camera works with **ALL** apps that access the camera, including:

- **Social Media**: Instagram, Snapchat, TikTok, Facebook
- **Video Conferencing**: Zoom, Google Meet, Microsoft Teams
- **Live Streaming**: YouTube Live, Twitch, Facebook Live
- **Online Sales**: Various e-commerce and sales apps
- **Camera Apps**: Default camera app, third-party camera apps
- **Any App**: That uses the Android camera system

## ⚠️ **Important Notes**

### **Root Access Required**
- This app requires root access to function properly
- Without root, the app cannot install system-level virtual camera
- Root access is needed to modify system files and intercept camera calls

### **Security Considerations**
- Only install on trusted devices
- Be aware that this modifies system behavior
- Use responsibly and in accordance with local laws

### **Compatibility**
- Works on Android 7.0 and above
- Requires ARM64 architecture
- Tested on various Android devices

## 🔧 **Technical Details**

### **How It Works**
1. **System-Level Hooking**: Uses native C++ code to hook into Android's camera system
2. **PLT Hooking**: Intercepts camera function calls at the system level
3. **Video Injection**: Replaces real camera feed with your video in real-time
4. **Root Access**: Required to modify system files and install hooks

### **Architecture**
- **SystemVirtualCamera**: Manages system-level virtual camera installation
- **Native C++ Hooks**: Intercept camera calls using PLT hooking
- **Video Processing**: Real-time video frame processing and injection
- **Root Integration**: Uses root access for system modification

## 🐛 **Troubleshooting**

### **"Device Not Rooted" Error**
- Ensure your device is properly rooted
- Check that root access is working with other root apps
- Try restarting the app after rooting

### **"Failed to Install System Camera" Error**
- Check root permissions
- Ensure the app has root access
- Try running the app as root

### **Virtual Camera Not Working in Apps**
- Ensure the system camera is installed
- Check that the virtual camera feed is started
- Try restarting the target app

### **Video Not Loading**
- Check that the video file exists
- Ensure the video path is correct
- Try using a different video format

## 📋 **Permissions Required**

- `CAMERA`: Access to camera hardware
- `RECORD_AUDIO`: Audio recording capabilities
- `WRITE_EXTERNAL_STORAGE`: Save video files
- `READ_EXTERNAL_STORAGE`: Read video files
- `SYSTEM_ALERT_WINDOW`: System-level access
- `WRITE_SETTINGS`: Modify system settings
- `MODIFY_PHONE_STATE`: System modification
- `WRITE_SECURE_SETTINGS`: Secure settings access
- `MOUNT_UNMOUNT_FILESYSTEMS`: File system access
- `ACCESS_SUPERUSER`: Root access

## 🚀 **Getting Started**

1. **Root your Android device** (if not already rooted)
2. **Download and install** the Virtual Camera app
3. **Grant root permissions** when prompted
4. **Install the system virtual camera** using the app
5. **Start the virtual camera feed** with your video
6. **Open any camera app** to see your video

## 📞 **Support**

If you encounter any issues:

1. Check that your device is properly rooted
2. Ensure all permissions are granted
3. Try restarting the app and device
4. Check the Android logs for error messages

## ⚖️ **Legal Notice**

- Use this app responsibly and in accordance with local laws
- Respect privacy and consent when using virtual cameras
- This app is for educational and legitimate purposes only
- The developers are not responsible for misuse of this app

---

**Enjoy using your virtual camera for live streaming, online sales, and more!** 🎥✨
