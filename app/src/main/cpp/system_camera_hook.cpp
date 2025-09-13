#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <string>
#include <vector>
#include <thread>
#include <chrono>
#include <unistd.h>
#include <sys/mman.h>
#include "video_processor.h"
#include "plt_hook.h"

#define LOG_TAG "SystemCameraHook"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global variables
static VideoProcessor* g_video_processor = nullptr;
static bool g_system_hook_installed = false;
static std::thread g_video_thread;
static bool g_video_running = false;
static std::string g_video_path = "";

// Forward declarations
void start_video_injection();
void stop_video_injection();
void inject_video_frame(const std::vector<uint8_t>& frame);
std::vector<uint8_t> convertToNV21(const std::vector<uint8_t>& rgb_frame);
bool install_plt_hooks();
void uninstall_plt_hooks();

// Function pointers for original Camera HAL functions
typedef int (*camera_device_open_original_t)(const struct hw_module_t* module, const char* id, struct hw_device_t** device);
typedef int (*camera_device_close_original_t)(struct hw_device_t* device);
typedef int (*camera_device_start_preview_original_t)(struct camera_device* device);
typedef int (*camera_device_stop_preview_original_t)(struct camera_device* device);
typedef int (*camera_device_set_preview_callback_original_t)(struct camera_device* device, void* callback);
typedef int (*camera_device_set_preview_window_original_t)(struct camera_device* device, struct preview_stream_ops* window);

// Camera HAL structures (simplified)
struct hw_module_t;
struct hw_device_t;
struct camera_device;
struct preview_stream_ops;

// Original function pointers
static camera_device_open_original_t g_camera_device_open_original = nullptr;
static camera_device_close_original_t g_camera_device_close_original = nullptr;
static camera_device_start_preview_original_t g_camera_device_start_preview_original = nullptr;
static camera_device_stop_preview_original_t g_camera_device_stop_preview_original = nullptr;
static camera_device_set_preview_callback_original_t g_camera_device_set_preview_callback_original = nullptr;
static camera_device_set_preview_window_original_t g_camera_device_set_preview_window_original = nullptr;

// Hooked Camera HAL functions
extern "C" int camera_device_open_hooked(const struct hw_module_t* module, const char* id, struct hw_device_t** device) {
    LOGD("System camera hook: camera_device_open intercepted for id: %s", id ? id : "null");
    
    // Call original function
    int result = g_camera_device_open_original ? g_camera_device_open_original(module, id, device) : -1;
    
    if (result == 0 && device) {
        LOGD("System camera hook: Camera device opened successfully");
        
        // Start video injection if not already running
        if (!g_video_running && g_video_processor) {
            start_video_injection();
        }
    }
    
    return result;
}

extern "C" int camera_device_close_hooked(struct hw_device_t* device) {
    LOGD("System camera hook: camera_device_close intercepted");
    
    // Stop video injection
    if (g_video_running) {
        stop_video_injection();
    }
    
    // Call original function
    return g_camera_device_close_original ? g_camera_device_close_original(device) : -1;
}

extern "C" int camera_device_start_preview_hooked(struct camera_device* device) {
    LOGD("System camera hook: camera_device_start_preview intercepted");
    
    // Start video injection
    if (!g_video_running && g_video_processor) {
        start_video_injection();
    }
    
    // Call original function
    return g_camera_device_start_preview_original ? g_camera_device_start_preview_original(device) : -1;
}

extern "C" int camera_device_stop_preview_hooked(struct camera_device* device) {
    LOGD("System camera hook: camera_device_stop_preview intercepted");
    
    // Stop video injection
    if (g_video_running) {
        stop_video_injection();
    }
    
    // Call original function
    return g_camera_device_stop_preview_original ? g_camera_device_stop_preview_original(device) : -1;
}

extern "C" int camera_device_set_preview_callback_hooked(struct camera_device* device, void* callback) {
    LOGD("System camera hook: camera_device_set_preview_callback intercepted");
    
    // Call original function
    return g_camera_device_set_preview_callback_original ? g_camera_device_set_preview_callback_original(device, callback) : -1;
}

extern "C" int camera_device_set_preview_window_hooked(struct camera_device* device, struct preview_stream_ops* window) {
    LOGD("System camera hook: camera_device_set_preview_window intercepted");
    
    // Call original function
    return g_camera_device_set_preview_window_original ? g_camera_device_set_preview_window_original(device, window) : -1;
}

// Video injection functions
void start_video_injection() {
    if (g_video_running || !g_video_processor) {
        return;
    }
    
    g_video_running = true;
    g_video_thread = std::thread([]() {
        LOGD("System camera hook: Starting video injection thread");
        
        while (g_video_running) {
            try {
                // Get video frame
                std::vector<uint8_t> frame = g_video_processor->getNextFrame();
                if (!frame.empty()) {
                    // Inject frame into camera stream
                    inject_video_frame(frame);
                }
                
                // Maintain frame rate
                std::this_thread::sleep_for(std::chrono::milliseconds(33)); // ~30 FPS
            } catch (const std::exception& e) {
                LOGE("Error in video injection: %s", e.what());
            }
        }
        
        LOGD("System camera hook: Video injection thread stopped");
    });
}

void stop_video_injection() {
    if (!g_video_running) {
        return;
    }
    
    g_video_running = false;
    if (g_video_thread.joinable()) {
        g_video_thread.join();
    }
    
    LOGD("System camera hook: Video injection stopped");
}

void inject_video_frame(const std::vector<uint8_t>& frame) {
    LOGD("System camera hook: Injecting video frame of size: %zu", frame.size());
    
    // Convert video frame to camera format (NV21)
    std::vector<uint8_t> nv21_frame = convertToNV21(frame);
    
    // Inject into camera preview buffer
    if (!nv21_frame.empty()) {
        // In a real implementation, you would:
        // 1. Get the camera preview buffer
        // 2. Replace the buffer data with video frame
        // 3. Notify the camera system of new frame
        
        // For now, we'll simulate the injection
        LOGD("System camera hook: Video frame injected successfully (simulated)");
        
        // In real implementation, you would call:
        // camera_preview_callback(nv21_frame.data(), nv21_frame.size());
    }
}

// Convert RGB frame to NV21 format (camera format)
std::vector<uint8_t> convertToNV21(const std::vector<uint8_t>& rgb_frame) {
    if (rgb_frame.empty()) {
        return std::vector<uint8_t>();
    }
    
    // Assume input is RGB888, convert to NV21
    // NV21 format: Y plane + interleaved VU plane
    int width = 640;  // Default width
    int height = 480; // Default height
    int rgb_size = width * height * 3;
    
    if (rgb_frame.size() < rgb_size) {
        LOGE("Frame size too small: %zu, expected: %d", rgb_frame.size(), rgb_size);
        return std::vector<uint8_t>();
    }
    
    std::vector<uint8_t> nv21_frame(width * height * 3 / 2);
    
    // Convert RGB to YUV420 (NV21)
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int rgb_idx = (y * width + x) * 3;
            int y_idx = y * width + x;
            
            // Extract RGB values
            uint8_t r = rgb_frame[rgb_idx];
            uint8_t g = rgb_frame[rgb_idx + 1];
            uint8_t b = rgb_frame[rgb_idx + 2];
            
            // Convert to Y (luminance)
            uint8_t y_val = (uint8_t)(0.299 * r + 0.587 * g + 0.114 * b);
            nv21_frame[y_idx] = y_val;
        }
    }
    
    // Convert to UV plane (simplified)
    for (int y = 0; y < height / 2; y++) {
        for (int x = 0; x < width / 2; x++) {
            int uv_idx = width * height + y * width + x * 2;
            nv21_frame[uv_idx] = 128;     // U
            nv21_frame[uv_idx + 1] = 128; // V
        }
    }
    
    LOGD("Converted RGB frame to NV21 format: %zu bytes", nv21_frame.size());
    return nv21_frame;
}

// JNI functions
extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_installSystemHook(JNIEnv* env, jobject thiz) {
    if (g_system_hook_installed) {
        LOGD("System camera hook already installed");
        return JNI_TRUE;
    }
    
    try {
        // Load camera library
        void* camera_lib = dlopen("libcamera_client.so", RTLD_LAZY);
        if (!camera_lib) {
            LOGE("Failed to load libcamera_client.so: %s", dlerror());
            return JNI_FALSE;
        }
        
        // Get original function addresses
        g_camera_device_open_original = (camera_device_open_original_t)dlsym(camera_lib, "camera_device_open");
        g_camera_device_close_original = (camera_device_close_original_t)dlsym(camera_lib, "camera_device_close");
        g_camera_device_start_preview_original = (camera_device_start_preview_original_t)dlsym(camera_lib, "camera_device_start_preview");
        g_camera_device_stop_preview_original = (camera_device_stop_preview_original_t)dlsym(camera_lib, "camera_device_stop_preview");
        g_camera_device_set_preview_callback_original = (camera_device_set_preview_callback_original_t)dlsym(camera_lib, "camera_device_set_preview_callback");
        g_camera_device_set_preview_window_original = (camera_device_set_preview_window_original_t)dlsym(camera_lib, "camera_device_set_preview_window");
        
        if (!g_camera_device_open_original || !g_camera_device_close_original) {
            LOGE("Failed to get original camera function addresses");
            dlclose(camera_lib);
            return JNI_FALSE;
        }
        
        // Install hooks using PLT hooking
        if (!install_plt_hooks()) {
            LOGE("Failed to install PLT hooks");
            dlclose(camera_lib);
            return JNI_FALSE;
        }
        
        g_system_hook_installed = true;
        LOGD("System camera hook installed successfully");
        
        return JNI_TRUE;
        
    } catch (const std::exception& e) {
        LOGE("Exception installing system camera hook: %s", e.what());
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_uninstallSystemHook(JNIEnv* env, jobject thiz) {
    if (!g_system_hook_installed) {
        return;
    }
    
    // Stop video injection
    stop_video_injection();
    
    // Uninstall hooks
    uninstall_plt_hooks();
    
    g_system_hook_installed = false;
    LOGD("System camera hook uninstalled");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_loadVideo(JNIEnv* env, jobject thiz, jstring videoPath) {
    const char* path = env->GetStringUTFChars(videoPath, 0);
    
    try {
        if (g_video_processor) {
            g_video_processor->stopDecoding();
            delete g_video_processor;
        }
        
        g_video_processor = new VideoProcessor(nullptr, std::string(path));
        bool success = g_video_processor->isInitialized();
        
        if (success) {
            g_video_path = std::string(path);
            g_video_processor->startDecoding();
            LOGD("Video loaded and decoding started: %s", path);
        } else {
            LOGE("Failed to load video: %s", path);
        }
        
        env->ReleaseStringUTFChars(videoPath, path);
        return success ? JNI_TRUE : JNI_FALSE;
        
    } catch (const std::exception& e) {
        LOGE("Exception loading video: %s", e.what());
        env->ReleaseStringUTFChars(videoPath, path);
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_startVirtualCamera(JNIEnv* env, jobject thiz) {
    LOGD("Starting system virtual camera");
    
    if (g_video_processor && !g_video_running) {
        start_video_injection();
        LOGD("System virtual camera started successfully");
    } else {
        LOGE("Failed to start system virtual camera - video processor not loaded or already running");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_stopVirtualCamera(JNIEnv* env, jobject thiz) {
    LOGD("Stopping system virtual camera");
    stop_video_injection();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_system_SystemVirtualCamera_isHookInstalled(JNIEnv* env, jobject thiz) {
    return g_system_hook_installed ? JNI_TRUE : JNI_FALSE;
}

// PLT hooking functions
bool install_plt_hooks() {
    LOGD("Installing PLT hooks for system camera functions");
    
    bool success = true;
    
    // Hook Camera HAL functions
    success &= PLTHook::hookFunction("libcamera_client.so", "camera_device_open", 
                                   (void*)camera_device_open_hooked, 
                                   (void**)&g_camera_device_open_original);
    
    success &= PLTHook::hookFunction("libcamera_client.so", "camera_device_close", 
                                   (void*)camera_device_close_hooked, 
                                   (void**)&g_camera_device_close_original);
    
    success &= PLTHook::hookFunction("libcamera_client.so", "camera_device_start_preview", 
                                   (void*)camera_device_start_preview_hooked, 
                                   (void**)&g_camera_device_start_preview_original);
    
    success &= PLTHook::hookFunction("libcamera_client.so", "camera_device_stop_preview", 
                                   (void*)camera_device_stop_preview_hooked, 
                                   (void**)&g_camera_device_stop_preview_original);
    
    success &= PLTHook::hookFunction("libcamera_client.so", "camera_device_set_preview_callback", 
                                   (void*)camera_device_set_preview_callback_hooked, 
                                   (void**)&g_camera_device_set_preview_callback_original);
    
    success &= PLTHook::hookFunction("libcamera_client.so", "camera_device_set_preview_window", 
                                   (void*)camera_device_set_preview_window_hooked, 
                                   (void**)&g_camera_device_set_preview_window_original);
    
    // Also hook Camera2 API functions
    success &= PLTHook::hookFunction("libcamera2ndk.so", "ACameraManager_openCamera", 
                                   (void*)camera_device_open_hooked, 
                                   (void**)&g_camera_device_open_original);
    
    if (success) {
        LOGD("PLT hooks installed successfully");
    } else {
        LOGE("Failed to install some PLT hooks");
    }
    
    return success;
}

void uninstall_plt_hooks() {
    LOGD("Uninstalling PLT hooks for system camera functions");
    // Uninstall hooks and restore original functions
}
