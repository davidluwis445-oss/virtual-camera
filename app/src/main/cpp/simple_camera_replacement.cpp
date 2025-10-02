#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaCodec.h>
#include <string>
#include <thread>
#include <vector>
#include <mutex>

#define LOG_TAG "SimpleCameraReplacement"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Simple global state
static std::string g_video_path = "";
static bool g_camera_active = false;
static std::thread g_camera_thread;
static std::vector<uint8_t> g_current_frame;
static std::mutex g_frame_mutex;
static ANativeWindow* g_camera_window = nullptr;

// Simple video frame structure
struct SimpleVideoFrame {
    std::vector<uint8_t> data;
    int width;
    int height;
};

static std::vector<SimpleVideoFrame> g_video_frames;
static size_t g_current_frame_index = 0;

// Generate a simple test pattern that looks like a video
std::vector<uint8_t> generate_simple_test_pattern(int frame_number) {
    int width = 640;
    int height = 480;
    std::vector<uint8_t> frame(width * height * 4); // RGBA
    
    // Create a moving pattern that changes with frame number
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int idx = (y * width + x) * 4;
            
            // Create animated colors based on position and frame
            float time = frame_number * 0.1f;
            int r = (int)(128 + 127 * sin(x * 0.02f + time));
            int g = (int)(128 + 127 * sin(y * 0.02f + time + 2.0f));
            int b = (int)(128 + 127 * sin((x + y) * 0.02f + time + 4.0f));
            
            frame[idx] = r;     // R
            frame[idx + 1] = g; // G
            frame[idx + 2] = b; // B
            frame[idx + 3] = 255; // A
        }
    }
    
    return frame;
}

// Simple camera thread that provides video frames
void simple_camera_thread() {
    LOGD("Simple camera thread started");
    
    int frame_number = 0;
    while (g_camera_active) {
        try {
            // Generate or get video frame
            std::vector<uint8_t> frame_data;
            
            if (!g_video_frames.empty()) {
                // Use loaded video frames
                const auto& frame = g_video_frames[g_current_frame_index];
                frame_data = frame.data;
                g_current_frame_index = (g_current_frame_index + 1) % g_video_frames.size();
            } else {
                // Generate test pattern
                frame_data = generate_simple_test_pattern(frame_number++);
            }
            
            // Store current frame for external access
            {
                std::lock_guard<std::mutex> lock(g_frame_mutex);
                g_current_frame = frame_data;
            }
            
            // Render to window if available
            if (g_camera_window) {
                ANativeWindow_Buffer buffer;
                if (ANativeWindow_lock(g_camera_window, &buffer, nullptr) == 0) {
                    // Copy frame data to window buffer
                    if (buffer.bits && frame_data.size() >= buffer.width * buffer.height * 4) {
                        memcpy(buffer.bits, frame_data.data(), 
                               std::min((size_t)(buffer.width * buffer.height * 4), frame_data.size()));
                    }
                    ANativeWindow_unlockAndPost(g_camera_window);
                }
            }
            
            // 30 FPS
            std::this_thread::sleep_for(std::chrono::milliseconds(33));
            
        } catch (const std::exception& e) {
            LOGE("Error in camera thread: %s", e.what());
        }
    }
    
    LOGD("Simple camera thread stopped");
}

// Load video file (simplified)
bool load_simple_video(const std::string& video_path) {
    LOGD("Loading video: %s", video_path.c_str());
    
    try {
        // For now, create some test frames
        // In a real implementation, you would use MediaExtractor
        g_video_frames.clear();
        
        // Create 60 frames (2 seconds at 30fps)
        for (int i = 0; i < 60; i++) {
            SimpleVideoFrame frame;
            frame.width = 640;
            frame.height = 480;
            frame.data = generate_simple_test_pattern(i);
            g_video_frames.push_back(std::move(frame));
        }
        
        g_current_frame_index = 0;
        LOGD("Loaded %zu video frames", g_video_frames.size());
        return true;
        
    } catch (const std::exception& e) {
        LOGE("Error loading video: %s", e.what());
        return false;
    }
}

// JNI Functions
extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_camera_SimpleCameraReplacement_nativeLoadVideo(JNIEnv* env, jobject thiz, jstring videoPath) {
    const char* path = env->GetStringUTFChars(videoPath, 0);
    g_video_path = std::string(path);
    
    bool success = load_simple_video(g_video_path);
    
    env->ReleaseStringUTFChars(videoPath, path);
    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_camera_SimpleCameraReplacement_startCamera(JNIEnv* env, jobject thiz) {
    if (g_camera_active) {
        LOGD("Camera already active");
        return;
    }
    
    LOGD("Starting simple camera replacement");
    g_camera_active = true;
    g_camera_thread = std::thread(simple_camera_thread);
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_camera_SimpleCameraReplacement_stopCamera(JNIEnv* env, jobject thiz) {
    if (!g_camera_active) {
        return;
    }
    
    LOGD("Stopping simple camera replacement");
    g_camera_active = false;
    
    if (g_camera_thread.joinable()) {
        g_camera_thread.join();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_camera_SimpleCameraReplacement_setSurface(JNIEnv* env, jobject thiz, jobject surface) {
    if (surface) {
        g_camera_window = ANativeWindow_fromSurface(env, surface);
        if (g_camera_window) {
            ANativeWindow_setBuffersGeometry(g_camera_window, 640, 480, WINDOW_FORMAT_RGBA_8888);
            LOGD("Camera surface set: 640x480");
        }
    } else {
        if (g_camera_window) {
            ANativeWindow_release(g_camera_window);
            g_camera_window = nullptr;
        }
    }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_app001_virtualcamera_camera_SimpleCameraReplacement_getCurrentFrame(JNIEnv* env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_frame_mutex);
    
    if (g_current_frame.empty()) {
        return nullptr;
    }
    
    jbyteArray result = env->NewByteArray(g_current_frame.size());
    env->SetByteArrayRegion(result, 0, g_current_frame.size(), 
                           reinterpret_cast<const jbyte*>(g_current_frame.data()));
    
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_camera_SimpleCameraReplacement_isCameraActive(JNIEnv* env, jobject thiz) {
    return g_camera_active ? JNI_TRUE : JNI_FALSE;
}
