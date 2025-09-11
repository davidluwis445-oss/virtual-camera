#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <string>
#include <vector>
#include <thread>
#include <chrono>
#include "video_processor.h"

#define LOG_TAG "Camera2Hook"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global variables
static VideoProcessor* g_video_processor = nullptr;
static bool g_hook_installed = false;
static std::thread g_video_thread;
static bool g_video_running = false;
static jobject g_camera_manager = nullptr;
static JavaVM* g_jvm = nullptr;

// Function to get JNI environment
JNIEnv* getJNIEnv() {
    JNIEnv* env = nullptr;
    if (g_jvm && g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) {
        return env;
    }
    return nullptr;
}

// Function to inject video frame into camera stream
void inject_video_frame_into_camera(jobject cameraDevice, const std::vector<uint8_t>& frame) {
    JNIEnv* env = getJNIEnv();
    if (!env || !cameraDevice) {
        return;
    }
    
    LOGD("Injecting video frame of size: %zu into camera", frame.size());
    
    // In a real implementation, you would:
    // 1. Convert frame to YUV format
    // 2. Create Image object with the frame data
    // 3. Inject it into the camera capture session
    // This is a simplified version for demonstration
}

// Function to start video streaming
void start_video_streaming(jobject cameraDevice) {
    if (g_video_processor && !g_video_running) {
        g_video_running = true;
        g_video_thread = std::thread([cameraDevice]() {
            while (g_video_running) {
                try {
                    // Get video frame
                    std::vector<uint8_t> frame = g_video_processor->getNextFrame();
                    if (!frame.empty()) {
                        // Inject frame into camera stream
                        inject_video_frame_into_camera(cameraDevice, frame);
                    }
                    std::this_thread::sleep_for(std::chrono::milliseconds(33)); // ~30 FPS
                } catch (const std::exception& e) {
                    LOGE("Error in video streaming: %s", e.what());
                }
            }
        });
    }
}

// Function to stop video streaming
void stop_video_streaming() {
    g_video_running = false;
    if (g_video_thread.joinable()) {
        g_video_thread.join();
    }
}

// JNI functions
extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_hook_Camera2Hook_installCamera2Hook(JNIEnv* env, jobject thiz, jobject cameraManager) {
    if (g_hook_installed) {
        LOGD("Camera2 hook already installed");
        return JNI_TRUE;
    }
    
    try {
        // Store JVM reference
        env->GetJavaVM(&g_jvm);
        
        // Store camera manager reference
        g_camera_manager = env->NewGlobalRef(cameraManager);
        
        g_hook_installed = true;
        LOGD("Camera2 hook installed successfully");
        
        return JNI_TRUE;
        
    } catch (const std::exception& e) {
        LOGE("Exception installing Camera2 hook: %s", e.what());
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_hook_Camera2Hook_uninstallCamera2Hook(JNIEnv* env, jobject thiz) {
    if (!g_hook_installed) {
        return;
    }
    
    // Stop video streaming
    stop_video_streaming();
    
    // Clean up references
    if (g_camera_manager) {
        env->DeleteGlobalRef(g_camera_manager);
        g_camera_manager = nullptr;
    }
    
    g_hook_installed = false;
    LOGD("Camera2 hook uninstalled");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_hook_Camera2Hook_loadVideo(JNIEnv* env, jobject thiz, jstring videoPath) {
    const char* path = env->GetStringUTFChars(videoPath, 0);
    
    try {
        if (g_video_processor) {
            delete g_video_processor;
        }
        
        g_video_processor = new VideoProcessor(nullptr, std::string(path));
        bool success = g_video_processor->isInitialized();
        
        env->ReleaseStringUTFChars(videoPath, path);
        
        if (success) {
            LOGD("Video loaded successfully: %s", path);
        } else {
            LOGE("Failed to load video: %s", path);
        }
        
        return success ? JNI_TRUE : JNI_FALSE;
        
    } catch (const std::exception& e) {
        LOGE("Exception loading video: %s", e.what());
        env->ReleaseStringUTFChars(videoPath, path);
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_hook_Camera2Hook_startVirtualCamera(JNIEnv* env, jobject thiz, jobject cameraDevice) {
    LOGD("Starting virtual camera with Camera2");
    
    if (g_video_processor && !g_video_running) {
        start_video_streaming(cameraDevice);
        LOGD("Virtual camera started successfully");
    } else {
        LOGE("Failed to start virtual camera - video processor not loaded or already running");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_hook_Camera2Hook_stopVirtualCamera(JNIEnv* env, jobject thiz) {
    LOGD("Stopping virtual camera");
    stop_video_streaming();
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_app001_virtualcamera_hook_Camera2Hook_getVideoInfo(JNIEnv* env, jobject thiz) {
    if (!g_video_processor) {
        return nullptr;
    }
    
    // Create VideoInfo object
    jclass videoInfoClass = env->FindClass("com/app001/virtualcamera/video/VideoInfo");
    jmethodID constructor = env->GetMethodID(videoInfoClass, "<init>", "(IIII)V");
    
    int width = g_video_processor->getWidth();
    int height = g_video_processor->getHeight();
    int frameRate = g_video_processor->getFrameRate();
    int currentFrame = 0; // You would track this in your VideoProcessor
    
    return env->NewObject(videoInfoClass, constructor, width, height, frameRate, currentFrame);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_hook_Camera2Hook_isHookInstalled(JNIEnv* env, jobject thiz) {
    return g_hook_installed ? JNI_TRUE : JNI_FALSE;
}
