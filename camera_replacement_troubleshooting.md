# 🔧 Camera App Replacement Troubleshooting Guide

## 🚨 **The Problem:**
Third-party apps (TikTok, Telegram, Instagram, etc.) are still not using your virtual camera and instead open the default system camera.

## ✅ **What I've Implemented:**

### **1. Enhanced Camera App Registration:**
- **High Priority Intent Filters**: Added `android:priority="1000"` to all camera intents
- **Comprehensive Intent Support**: Added support for all camera intents:
  - `android.media.action.IMAGE_CAPTURE`
  - `android.media.action.VIDEO_CAPTURE`
  - `android.media.action.IMAGE_CAPTURE_SECURE`
  - `android.media.action.VIDEO_CAPTURE_SECURE`
  - `android.intent.action.CAMERA`
  - `android.intent.action.CAMERA_BUTTON`

### **2. Camera Provider Services:**
- **CameraProviderService**: Intercepts camera requests
- **VirtualCameraProviderService**: Provides camera capabilities to the system
- **CameraIntentHandler**: Manages camera app selection and intent handling

### **3. Advanced Testing Tools:**
- **Test Launch Button**: Force launch virtual camera for testing
- **Default Camera App Detection**: Shows if app is set as default
- **Camera App List**: Shows all available camera apps

## 🔍 **Step-by-Step Troubleshooting:**

### **Step 1: Check if App is Registered as Camera App**
1. Open your Virtual Camera app
2. Go to **Advanced** tab
3. Look for **"Default Camera App"** status
4. If it shows **"❌"**, proceed to Step 2

### **Step 2: Set as Default Camera App**
1. In the **Advanced** tab, find the **"Set as Default Camera App"** card
2. Click **"Set as Default"** button
3. This opens Android Settings → Apps → Default Apps → Camera
4. Select **"Virtual Camera"** as the default camera app
5. Go back to your app and check if status shows **"✅"**

### **Step 3: Test Virtual Camera Launch**
1. In the **Advanced** tab, click **"Test Launch"** button
2. This should open your virtual camera directly
3. If it works, the virtual camera is functioning correctly
4. If it doesn't work, there's an issue with the app registration

### **Step 4: Test with Third-Party Apps**
1. **TikTok**: Create video → Camera → Should open your virtual camera
2. **Telegram**: Chat → Camera icon → Should open your virtual camera
3. **Instagram**: Stories → Camera → Should open your virtual camera
4. **WhatsApp**: Chat → Camera → Should open your virtual camera

## 🐛 **Common Issues and Solutions:**

### **Issue 1: App Not in Camera App List**
**Symptoms**: Virtual Camera doesn't appear in Android's camera app selection
**Solutions**:
1. **Reinstall the app** - Sometimes Android needs a fresh installation
2. **Check permissions** - Ensure all camera permissions are granted
3. **Restart device** - Android sometimes needs a restart to recognize new camera apps
4. **Check Android version** - Some older versions have different camera app handling

### **Issue 2: Default Camera App Not Sticking**
**Symptoms**: You set it as default but it reverts back
**Solutions**:
1. **Clear other camera apps** - Uninstall or disable other camera apps
2. **Check system camera** - Some devices have a built-in camera that can't be disabled
3. **Use ADB commands** - Force set the default camera app via ADB

### **Issue 3: Third-Party Apps Still Use System Camera**
**Symptoms**: TikTok, Telegram, etc. still open the system camera
**Solutions**:
1. **Force stop third-party apps** - Clear their cache and data
2. **Check app-specific settings** - Some apps have their own camera preferences
3. **Use ADB to test** - Test camera intents manually

## 🔧 **Advanced Troubleshooting:**

### **ADB Commands for Testing:**
```bash
# Test camera intent
adb shell am start -a android.media.action.IMAGE_CAPTURE

# List all camera apps
adb shell pm query-activities -a android.media.action.IMAGE_CAPTURE

# Check default camera app
adb shell settings get secure camera_default_application

# Set default camera app (requires root)
adb shell settings put secure camera_default_application com.app001.virtualcamera
```

### **Check App Registration:**
```bash
# Check if app is registered for camera intents
adb shell dumpsys package com.app001.virtualcamera | grep -i camera

# Check intent filters
adb shell dumpsys package com.app001.virtualcamera | grep -A 10 -B 10 "android.media.action.IMAGE_CAPTURE"
```

### **Log Analysis:**
```bash
# Monitor camera-related logs
adb logcat | grep -E "(VirtualCamera|CameraProvider|CameraIntent|IMAGE_CAPTURE)"

# Check for errors
adb logcat | grep -E "(ERROR|FATAL).*VirtualCamera"
```

## 📱 **Device-Specific Issues:**

### **Samsung Devices:**
- Samsung has its own camera app that's deeply integrated
- Try disabling Samsung Camera in Settings → Apps
- Use Samsung's camera app settings to change default

### **Xiaomi Devices:**
- MIUI has additional camera app management
- Check MIUI Settings → Apps → Default Apps → Camera
- Some MIUI versions require additional permissions

### **OnePlus Devices:**
- OxygenOS has camera app preferences
- Check Settings → Apps → Default Apps → Camera
- Some versions require root access for camera replacement

### **Google Pixel Devices:**
- Pixel devices have Google Camera as default
- Try disabling Google Camera
- Use Google's camera app settings

## 🎯 **Success Indicators:**

### **App Level:**
- ✅ **Advanced tab shows**: "Default Camera App: ✅"
- ✅ **Test Launch button**: Opens virtual camera successfully
- ✅ **Video plays**: Selected video plays in virtual camera

### **System Level:**
- ✅ **Camera app list**: Virtual Camera appears in Android's camera app selection
- ✅ **Default camera**: Virtual Camera is selected as default
- ✅ **Third-party apps**: TikTok, Telegram, etc. open Virtual Camera

### **Third-Party App Level:**
- ✅ **TikTok**: Create video → Camera → Opens Virtual Camera
- ✅ **Telegram**: Chat → Camera → Opens Virtual Camera
- ✅ **Instagram**: Stories → Camera → Opens Virtual Camera
- ✅ **WhatsApp**: Chat → Camera → Opens Virtual Camera

## 🚀 **Final Solution (If All Else Fails):**

If the above methods don't work, the issue might be that Android's camera app selection is more complex than just intent filters. In this case, you may need to:

1. **Root the device** - Some camera replacements require root access
2. **Use Xposed Framework** - For system-level camera hooking
3. **Modify system files** - Direct modification of camera app preferences
4. **Use a different approach** - Such as screen recording or overlay methods

## 📞 **Getting Help:**

If you're still having issues, please provide:
1. **Device information**: Make, model, Android version
2. **App status**: What the Advanced tab shows
3. **Test results**: Whether Test Launch works
4. **Logs**: Output from the ADB commands above
5. **Specific apps**: Which third-party apps are not working

---

**Remember**: Camera app replacement is a complex system-level operation that varies between Android versions and device manufacturers. The solution I've implemented should work on most devices, but some may require additional steps or root access.
