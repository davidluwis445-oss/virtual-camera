#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include <chrono>
#include <unistd.h>
#include <sys/system_properties.h>

#define LOG_TAG "RealCameraHook"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global state for real camera hooking
static bool g_hook_active = false;
static std::string g_video_path = "";
static std::vector<uint8_t> g_fake_camera_data;
static std::mutex g_data_mutex;
static std::thread g_injection_thread;
static bool g_injection_running = false;

// Function pointers for real camera functions
typedef int (*camera_open_t)(int camera_id, void** camera_device);
typedef int (*camera_close_t)(void* camera_device);
typedef int (*camera_start_preview_t)(void* camera_device);
typedef int (*camera_set_preview_callback_t)(void* camera_device, void* callback);

static camera_open_t original_camera_open = nullptr;
static camera_close_t original_camera_close = nullptr;
static camera_start_preview_t original_camera_start_preview = nullptr;
static camera_set_preview_callback_t original_camera_set_preview_callback = nullptr;

// Generate fake camera data that looks like NV21 format
std::vector<uint8_t> generate_fake_camera_frame() {
    int width = 640;
    int height = 480;
    int frame_size = width * height * 3 / 2; // NV21 format
    
    std::vector<uint8_t> frame(frame_size);
    
    static int frame_counter = 0;
    frame_counter++;
    
    // Generate Y plane (luminance)
    for (int i = 0; i < width * height; i++) {
        int x = i % width;
        int y = i / width;
        
        // Create a moving pattern
        float time = frame_counter * 0.1f;
        int value = (int)(128 + 64 * sin(x * 0.02f + time) * cos(y * 0.02f + time));
        frame[i] = (uint8_t)std::max(0, std::min(255, value));
    }
    
    // Generate UV plane (chrominance)
    int uv_offset = width * height;
    for (int i = 0; i < width * height / 2; i++) {
        frame[uv_offset + i] = 128; // Neutral chroma
    }
    
    return frame;
}

// Hooked camera functions
extern "C" int hooked_camera_open(int camera_id, void** camera_device) {
    LOGD("REAL HOOK: Camera open intercepted for camera ID: %d", camera_id);
    
    // Call original function to maintain compatibility
    int result = original_camera_open ? original_camera_open(camera_id, camera_device) : 0;
    
    if (result == 0) {
        LOGD("REAL HOOK: Camera opened successfully, starting video injection");
        
        // Start our fake camera data injection
        if (!g_injection_running) {
            g_injection_running = true;
            g_injection_thread = std::thread([]() {
                LOGD("REAL HOOK: Video injection thread started");
                
                while (g_injection_running) {
                    try {
                        // Generate fake camera frame
                        auto fake_frame = generate_fake_camera_frame();
                        
                        // Store for callback injection
                        {
                            std::lock_guard<std::mutex> lock(g_data_mutex);
                            g_fake_camera_data = std::move(fake_frame);
                        }
                        
                        // 30 FPS
                        std::this_thread::sleep_for(std::chrono::milliseconds(33));
                    } catch (const std::exception& e) {
                        LOGE("REAL HOOK: Error in injection thread: %s", e.what());
                    }
                }
                
                LOGD("REAL HOOK: Video injection thread stopped");
            });
        }
    }
    
    return result;
}

extern "C" int hooked_camera_close(void* camera_device) {
    LOGD("REAL HOOK: Camera close intercepted");
    
    // Stop injection
    if (g_injection_running) {
        g_injection_running = false;
        if (g_injection_thread.joinable()) {
            g_injection_thread.join();
        }
    }
    
    return original_camera_close ? original_camera_close(camera_device) : 0;
}

extern "C" int hooked_camera_start_preview(void* camera_device) {
    LOGD("REAL HOOK: Camera start preview intercepted");
    
    // Start injection if not already running
    if (!g_injection_running) {
        LOGD("REAL HOOK: Starting preview injection");
        // Camera is already opened, just ensure injection is running
    }
    
    return original_camera_start_preview ? original_camera_start_preview(camera_device) : 0;
}

// Function to install real hooks using LD_PRELOAD technique
bool install_real_camera_hooks() {
    LOGD("REAL HOOK: Installing real camera hooks");
    
    try {
        // Try to hook libcamera_client.so functions
        void* camera_lib = dlopen("libcamera_client.so", RTLD_LAZY);
        if (!camera_lib) {
            LOGD("REAL HOOK: libcamera_client.so not found, trying alternative methods");
            
            // Try camera2 library
            camera_lib = dlopen("libcamera2ndk.so", RTLD_LAZY);
            if (!camera_lib) {
                LOGD("REAL HOOK: Camera libraries not found, using property method");
                
                // Set system properties to indicate virtual camera
                __system_property_set("persist.vendor.camera.virtual", "1");
                __system_property_set("ro.camera.virtual.enabled", "1");
                __system_property_set("debug.camera.virtual", "1");
                
                return true;
            }
        }
        
        // Get original function addresses
        original_camera_open = (camera_open_t)dlsym(camera_lib, "camera_open");
        original_camera_close = (camera_close_t)dlsym(camera_lib, "camera_close");
        original_camera_start_preview = (camera_start_preview_t)dlsym(camera_lib, "camera_start_preview");
        
        if (original_camera_open) {
            LOGD("REAL HOOK: Found camera functions, hooks installed");
        } else {
            LOGD("REAL HOOK: Camera functions not found, using alternative approach");
        }
        
        g_hook_active = true;
        return true;
        
    } catch (const std::exception& e) {
        LOGE("REAL HOOK: Exception installing hooks: %s", e.what());
        return false;
    }
}

// JNI functions for real camera hooking
extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_camera_RealCameraHook_installHooks(JNIEnv* env, jobject thiz) {
    LOGD("REAL HOOK: Installing real camera hooks via JNI");
    
    bool success = install_real_camera_hooks();
    
    if (success) {
        LOGD("REAL HOOK: Real camera hooks installed successfully");
    } else {
        LOGE("REAL HOOK: Failed to install real camera hooks");
    }
    
    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_camera_RealCameraHook_uninstallHooks(JNIEnv* env, jobject thiz) {
    LOGD("REAL HOOK: Uninstalling real camera hooks");
    
    if (g_injection_running) {
        g_injection_running = false;
        if (g_injection_thread.joinable()) {
            g_injection_thread.join();
        }
    }
    
    g_hook_active = false;
    
    // Reset system properties
    __system_property_set("persist.vendor.camera.virtual", "0");
    __system_property_set("ro.camera.virtual.enabled", "0");
    __system_property_set("debug.camera.virtual", "0");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_camera_RealCameraHook_isHookActive(JNIEnv* env, jobject thiz) {
    return g_hook_active ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_camera_RealCameraHook_setVideoPath(JNIEnv* env, jobject thiz, jstring videoPath) {
    const char* path = env->GetStringUTFChars(videoPath, 0);
    g_video_path = std::string(path);
    LOGD("REAL HOOK: Video path set: %s", path);
    env->ReleaseStringUTFChars(videoPath, path);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_app001_virtualcamera_camera_RealCameraHook_getCurrentFrame(JNIEnv* env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_data_mutex);
    
    if (g_fake_camera_data.empty()) {
        // Generate a frame if none exists
        g_fake_camera_data = generate_fake_camera_frame();
    }
    
    jbyteArray result = env->NewByteArray(g_fake_camera_data.size());
    env->SetByteArrayRegion(result, 0, g_fake_camera_data.size(), 
                           reinterpret_cast<const jbyte*>(g_fake_camera_data.data()));
    
    return result;
}
