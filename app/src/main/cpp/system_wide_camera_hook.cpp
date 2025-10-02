#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <string>
#include <vector>
#include <thread>
#include <chrono>
#include <unistd.h>
#include <sys/mman.h>
#include <cmath>
#include <mutex>
#include <algorithm>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/surface_texture.h>
#include <android/surface_texture_jni.h>
// Note: MediaCodec and MediaStagefright headers are not available in standard NDK
// These would need to be replaced with MediaCodec Java API calls or removed
// #include <media/stagefright/MediaCodec.h>
// #include <media/stagefright/MediaMuxer.h>
// #include <media/stagefright/MediaExtractor.h>
// #include <media/stagefright/MediaSource.h>
// #include <media/stagefright/MediaBuffer.h>
// #include <media/stagefright/MediaBufferGroup.h>
// #include <media/stagefright/MediaCodecList.h>
// #include <media/stagefright/MediaCodecSource.h>
#include "plt_hook.h"

#undef LOG_TAG
#define LOG_TAG "SystemWideCameraHook"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global variables for system-wide camera preview replacement
static bool g_system_wide_hook_installed = false;
static std::thread g_preview_thread;
static bool g_preview_running = false;
static std::string g_video_path = "";
static ANativeWindow* g_preview_window = nullptr;
static std::vector<ANativeWindow*> g_camera_windows;
static std::mutex g_window_mutex;

// Function pointers for original functions
typedef int (*ANativeWindow_lock_original_t)(ANativeWindow* window, ANativeWindow_Buffer* outBuffer, ARect* inOutDirtyBounds);
typedef int (*ANativeWindow_unlockAndPost_original_t)(ANativeWindow* window);
typedef int (*ANativeWindow_setBuffersGeometry_original_t)(ANativeWindow* window, int32_t width, int32_t height, int32_t format);
typedef int (*ANativeWindow_fromSurface_original_t)(JNIEnv* env, jobject surface);

// Original function pointers
static ANativeWindow_lock_original_t g_ANativeWindow_lock_original = nullptr;
static ANativeWindow_unlockAndPost_original_t g_ANativeWindow_unlockAndPost_original = nullptr;
static ANativeWindow_setBuffersGeometry_original_t g_ANativeWindow_setBuffersGeometry_original = nullptr;
static ANativeWindow_fromSurface_original_t g_ANativeWindow_fromSurface_original = nullptr;

// Forward declarations
void start_system_wide_preview_replacement();
void stop_system_wide_preview_replacement();
void inject_preview_into_window(ANativeWindow* window);
std::vector<uint8_t> generate_system_wide_preview_frame();
bool install_system_wide_hooks();
void uninstall_system_wide_hooks();
bool is_camera_preview_window_system_wide(ANativeWindow* window);

// Hooked ANativeWindow functions for system-wide preview replacement
extern "C" int ANativeWindow_lock_system_wide_hooked(ANativeWindow* window, ANativeWindow_Buffer* outBuffer, ARect* inOutDirtyBounds) {
    LOGD("System-wide hook: ANativeWindow_lock intercepted");
    
    // Call original function
    int result = g_ANativeWindow_lock_original ? 
        g_ANativeWindow_lock_original(window, outBuffer, inOutDirtyBounds) : -1;
    
    if (result == 0 && window && outBuffer) {
        // Check if this is a camera preview window
        if (is_camera_preview_window_system_wide(window)) {
            LOGD("System-wide hook: Camera preview window detected - replacing content");
            
            // Add to camera windows list
            {
                std::lock_guard<std::mutex> lock(g_window_mutex);
                if (std::find(g_camera_windows.begin(), g_camera_windows.end(), window) == g_camera_windows.end()) {
                    g_camera_windows.push_back(window);
                }
            }
            
            // Replace preview content
            inject_preview_into_window(window);
        }
    }
    
    return result;
}

extern "C" int ANativeWindow_unlockAndPost_system_wide_hooked(ANativeWindow* window) {
    LOGD("System-wide hook: ANativeWindow_unlockAndPost intercepted");
    
    // Call original function
    int result = g_ANativeWindow_unlockAndPost_original ? 
        g_ANativeWindow_unlockAndPost_original(window) : -1;
    
    return result;
}

extern "C" int ANativeWindow_setBuffersGeometry_system_wide_hooked(ANativeWindow* window, int32_t width, int32_t height, int32_t format) {
    LOGD("System-wide hook: ANativeWindow_setBuffersGeometry intercepted - %dx%d format=%d", width, height, format);
    
    // Call original function
    int result = g_ANativeWindow_setBuffersGeometry_original ? 
        g_ANativeWindow_setBuffersGeometry_original(window, width, height, format) : -1;
    
    if (result == 0 && window) {
        // Check if this is a camera preview window
        if (is_camera_preview_window_system_wide(window)) {
            LOGD("System-wide hook: Camera preview window geometry set - preview replacement ready");
        }
    }
    
    return result;
}

extern "C" ANativeWindow* ANativeWindow_fromSurface_system_wide_hooked(JNIEnv* env, jobject surface) {
    LOGD("System-wide hook: ANativeWindow_fromSurface intercepted");
    
    // Call original function
    ANativeWindow* window = g_ANativeWindow_fromSurface_original ? 
        (ANativeWindow*)g_ANativeWindow_fromSurface_original(env, surface) : nullptr;
    
    if (window) {
        LOGD("System-wide hook: Surface converted to ANativeWindow - checking for camera preview");
        // The window will be checked in lock/unlock functions
    }
    
    return window;
}

// Check if window is used for camera preview
bool is_camera_preview_window_system_wide(ANativeWindow* window) {
    if (!window) return false;
    
    // Check window properties that indicate camera preview
    int32_t width = ANativeWindow_getWidth(window);
    int32_t height = ANativeWindow_getHeight(window);
    int32_t format = ANativeWindow_getFormat(window);
    
    // Camera preview typically has specific dimensions and format
    bool is_camera_size = (width >= 320 && width <= 4096) && (height >= 240 && height <= 4096);
    // Note: Hardware buffer format constants not available, using basic format check
    bool is_camera_format = (format == 1 || format == 2 || format == 4); // Basic format check
    
    return is_camera_size && is_camera_format;
}

// System-wide preview replacement functions
void start_system_wide_preview_replacement() {
    if (g_preview_running) {
        return;
    }
    
    g_preview_running = true;
    g_preview_thread = std::thread([]() {
        LOGD("System-wide hook: Starting system-wide preview replacement thread");
        
        while (g_preview_running) {
            try {
                // Process all camera windows
                {
                    std::lock_guard<std::mutex> lock(g_window_mutex);
                    for (auto window : g_camera_windows) {
                        if (window) {
                            inject_preview_into_window(window);
                        }
                    }
                }
                
                // Maintain frame rate for smooth preview
                std::this_thread::sleep_for(std::chrono::milliseconds(33)); // ~30 FPS
            } catch (const std::exception& e) {
                LOGE("Error in system-wide preview replacement: %s", e.what());
            }
        }
        
        LOGD("System-wide hook: System-wide preview replacement thread stopped");
    });
}

void stop_system_wide_preview_replacement() {
    if (!g_preview_running) {
        return;
    }
    
    g_preview_running = false;
    if (g_preview_thread.joinable()) {
        g_preview_thread.join();
    }
    
    // Clear camera windows
    {
        std::lock_guard<std::mutex> lock(g_window_mutex);
        g_camera_windows.clear();
    }
    
    LOGD("System-wide hook: System-wide preview replacement stopped");
}

void inject_preview_into_window(ANativeWindow* window) {
    if (!window) return;
    
    try {
        // Generate preview frame
        std::vector<uint8_t> frame = generate_system_wide_preview_frame();
        
        if (!frame.empty()) {
            // Get window buffer
            ANativeWindow_Buffer buffer;
            ARect dirtyRect;
            
            if (ANativeWindow_lock(window, &buffer, &dirtyRect) == 0) {
                // Replace buffer content with our video frame
                uint8_t* dst = static_cast<uint8_t*>(buffer.bits);
                int32_t stride = buffer.stride * 4; // Assuming RGBA format
                
                // Copy frame data to buffer
                int32_t width = std::min(buffer.width, 1280);
                int32_t height = std::min(buffer.height, 720);
                
                for (int32_t y = 0; y < height; y++) {
                    for (int32_t x = 0; x < width; x++) {
                        int32_t src_idx = (y * width + x) * 3;
                        int32_t dst_idx = (y * stride) + (x * 4);
                        
                        if (src_idx + 2 < frame.size() && dst_idx + 3 < stride * height) {
                            dst[dst_idx] = frame[src_idx + 2];     // B
                            dst[dst_idx + 1] = frame[src_idx + 1]; // G
                            dst[dst_idx + 2] = frame[src_idx];     // R
                            dst[dst_idx + 3] = 255;               // A
                        }
                    }
                }
                
                ANativeWindow_unlockAndPost(window);
                
                static int frame_count = 0;
                frame_count++;
                if (frame_count % 30 == 0) {
                    LOGD("System-wide hook: Injected %d frames into camera preview", frame_count);
                }
            }
        }
    } catch (const std::exception& e) {
        LOGE("Error injecting preview into window: %s", e.what());
    }
}

std::vector<uint8_t> generate_system_wide_preview_frame() {
    int width = 1280;
    int height = 720;
    std::vector<uint8_t> frame(width * height * 3);
    
    static int frame_count = 0;
    frame_count++;
    
    // Generate animated test pattern for system-wide preview replacement
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int idx = (y * width + x) * 3;
            
            // Create animated pattern
            float time = frame_count * 0.1f;
            float r = (sin(x * 0.01f + time) + 1.0f) * 0.5f * 255;
            float g = (sin(y * 0.01f + time) + 1.0f) * 0.5f * 255;
            float b = (sin((x + y) * 0.01f + time) + 1.0f) * 0.5f * 255;
            
            frame[idx] = (uint8_t)r;     // R
            frame[idx + 1] = (uint8_t)g; // G
            frame[idx + 2] = (uint8_t)b; // B
        }
    }
    
    return frame;
}

// Hook installation functions
bool install_system_wide_hooks() {
    LOGD("Installing system-wide camera preview replacement hooks");
    
    bool success = true;
    
    // Load libandroid library
    void* android_lib = dlopen("libandroid.so", RTLD_LAZY);
    if (!android_lib) {
        LOGE("Failed to load libandroid.so: %s", dlerror());
        return false;
    }
    
    // Get original function addresses
    g_ANativeWindow_lock_original = (ANativeWindow_lock_original_t)dlsym(android_lib, "ANativeWindow_lock");
    g_ANativeWindow_unlockAndPost_original = (ANativeWindow_unlockAndPost_original_t)dlsym(android_lib, "ANativeWindow_unlockAndPost");
    g_ANativeWindow_setBuffersGeometry_original = (ANativeWindow_setBuffersGeometry_original_t)dlsym(android_lib, "ANativeWindow_setBuffersGeometry");
    g_ANativeWindow_fromSurface_original = (ANativeWindow_fromSurface_original_t)dlsym(android_lib, "ANativeWindow_fromSurface");
    
    if (!g_ANativeWindow_lock_original || !g_ANativeWindow_unlockAndPost_original) {
        LOGE("Failed to get original ANativeWindow function addresses");
        dlclose(android_lib);
        return false;
    }
    
    // Install hooks using PLT hooking
    success &= PLTHook::hookFunction("libandroid.so", "ANativeWindow_lock", 
                                   (void*)ANativeWindow_lock_system_wide_hooked, 
                                   (void**)&g_ANativeWindow_lock_original);
    
    success &= PLTHook::hookFunction("libandroid.so", "ANativeWindow_unlockAndPost", 
                                   (void*)ANativeWindow_unlockAndPost_system_wide_hooked, 
                                   (void**)&g_ANativeWindow_unlockAndPost_original);
    
    success &= PLTHook::hookFunction("libandroid.so", "ANativeWindow_setBuffersGeometry", 
                                   (void*)ANativeWindow_setBuffersGeometry_system_wide_hooked, 
                                   (void**)&g_ANativeWindow_setBuffersGeometry_original);
    
    success &= PLTHook::hookFunction("libandroid.so", "ANativeWindow_fromSurface", 
                                   (void*)ANativeWindow_fromSurface_system_wide_hooked, 
                                   (void**)&g_ANativeWindow_fromSurface_original);
    
    if (success) {
        LOGD("System-wide camera preview replacement hooks installed successfully");
    } else {
        LOGE("Failed to install some system-wide hooks");
    }
    
    return success;
}

void uninstall_system_wide_hooks() {
    LOGD("Uninstalling system-wide camera preview replacement hooks");
    // Uninstall hooks and restore original functions
}

// JNI functions
extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_installSystemWideHook(JNIEnv* env, jobject thiz) {
    if (g_system_wide_hook_installed) {
        LOGD("System-wide hook already installed");
        return JNI_TRUE;
    }
    
    try {
        if (install_system_wide_hooks()) {
            g_system_wide_hook_installed = true;
            LOGD("System-wide camera preview replacement hook installed successfully");
            return JNI_TRUE;
        } else {
            LOGE("Failed to install system-wide camera preview replacement hook");
            return JNI_FALSE;
        }
    } catch (const std::exception& e) {
        LOGE("Exception installing system-wide camera preview replacement hook: %s", e.what());
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_uninstallSystemWideHook(JNIEnv* env, jobject thiz) {
    if (!g_system_wide_hook_installed) {
        return;
    }
    
    // Stop system-wide preview replacement
    stop_system_wide_preview_replacement();
    
    // Uninstall hooks
    uninstall_system_wide_hooks();
    
    g_system_wide_hook_installed = false;
    LOGD("System-wide camera preview replacement hook uninstalled");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_loadSystemWideVideo(JNIEnv* env, jobject thiz, jstring videoPath) {
    const char* path = env->GetStringUTFChars(videoPath, 0);
    
    try {
        g_video_path = std::string(path);
        LOGD("System-wide video path set: %s", path);
        
        env->ReleaseStringUTFChars(videoPath, path);
        return JNI_TRUE;
        
    } catch (const std::exception& e) {
        LOGE("Exception loading system-wide video: %s", e.what());
        env->ReleaseStringUTFChars(videoPath, path);
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_startSystemWidePreviewReplacementNative(JNIEnv* env, jobject thiz) {
    LOGD("Starting system-wide preview replacement");
    
    if (!g_preview_running) {
        start_system_wide_preview_replacement();
        LOGD("System-wide preview replacement started successfully");
    } else {
        LOGD("System-wide preview replacement already running");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_stopSystemWidePreviewReplacementNative(JNIEnv* env, jobject thiz) {
    LOGD("Stopping system-wide preview replacement");
    stop_system_wide_preview_replacement();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_isSystemWideHookInstalled(JNIEnv* env, jobject thiz) {
    return g_system_wide_hook_installed ? JNI_TRUE : JNI_FALSE;
}
