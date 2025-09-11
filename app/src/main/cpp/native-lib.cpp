#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include "video_processor.h"

#define LOG_TAG "NativeLib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Use malloc/free instead of new/delete
extern "C" JNIEXPORT jlong JNICALL
Java_com_app001_virtualcamera_VideoFileManager_initVideoProcessor(
        JNIEnv* env,
        jobject thiz,
        jobject assetManager,
        jstring videoPath) {

    const char* path = env->GetStringUTFChars(videoPath, 0);
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    // Allocate memory manually
    void* memory = malloc(sizeof(VideoProcessor));
    if (!memory) {
        env->ReleaseStringUTFChars(videoPath, path);
        return 0;
    }

    // Use placement new
    VideoProcessor* processor = new (memory) VideoProcessor(mgr, path);
    env->ReleaseStringUTFChars(videoPath, path);

    return reinterpret_cast<jlong>(processor);
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_VideoFileManager_releaseVideoProcessor(
        JNIEnv* env,
        jobject thiz,
        jlong processorPtr) {

    VideoProcessor* processor = reinterpret_cast<VideoProcessor*>(processorPtr);
    if (processor) {
        // Manually call destructor
        processor->~VideoProcessor();
        // Free memory
        free(processor);
    }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_app001_virtualcamera_VideoFileManager_getNextFrame(
        JNIEnv* env,
        jobject thiz,
        jlong processorPtr) {

    if (!processorPtr) {
        LOGE("Invalid processor pointer");
        return nullptr;
    }

    try {
        VideoProcessor* processor = reinterpret_cast<VideoProcessor*>(processorPtr);
        std::vector<uint8_t> frameData = processor->getNextFrame();

        if (frameData.empty()) {
            return nullptr;
        }

        jbyteArray result = env->NewByteArray(static_cast<jsize>(frameData.size()));
        if (result) {
            env->SetByteArrayRegion(result, 0, static_cast<jsize>(frameData.size()),
                                    reinterpret_cast<const jbyte*>(frameData.data()));
        }
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in getNextFrame: %s", e.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_app001_virtualcamera_VideoFileManager_getVideoWidth(
        JNIEnv* env,
        jobject thiz,
        jlong processorPtr) {

    if (!processorPtr) {
        LOGE("Invalid processor pointer");
        return -1;
    }

    VideoProcessor* processor = reinterpret_cast<VideoProcessor*>(processorPtr);
    return processor->getWidth();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_app001_virtualcamera_VideoFileManager_getVideoHeight(
        JNIEnv* env,
        jobject thiz,
        jlong processorPtr) {

    if (!processorPtr) {
        LOGE("Invalid processor pointer");
        return -1;
    }

    VideoProcessor* processor = reinterpret_cast<VideoProcessor*>(processorPtr);
    return processor->getHeight();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_app001_virtualcamera_VideoFileManager_getVideoFrameRate(
        JNIEnv* env,
        jobject thiz,
        jlong processorPtr) {

    if (!processorPtr) {
        LOGE("Invalid processor pointer");
        return -1;
    }

    VideoProcessor* processor = reinterpret_cast<VideoProcessor*>(processorPtr);
    return processor->getFrameRate();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_app001_virtualcamera_VideoFileManager_isVideoInitialized(
        JNIEnv* env,
        jobject thiz,
        jlong processorPtr) {

    if (!processorPtr) {
        return JNI_FALSE;
    }

    VideoProcessor* processor = reinterpret_cast<VideoProcessor*>(processorPtr);
    return processor->isInitialized() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_app001_virtualcamera_VideoFileManager_seekToTime(
        JNIEnv* env,
        jobject thiz,
        jlong processorPtr,
        jlong timeMs) {

    if (!processorPtr) {
        LOGE("Invalid processor pointer");
        return;
    }

    try {
        VideoProcessor* processor = reinterpret_cast<VideoProcessor*>(processorPtr);
        // Implement seek functionality in your VideoProcessor class
        // processor->seekTo(timeMs);
    } catch (const std::exception& e) {
        LOGE("Exception in seekToTime: %s", e.what());
    }
}

// For getNextFrame, you'll need to modify VideoProcessor to use C arrays instead of std::vector