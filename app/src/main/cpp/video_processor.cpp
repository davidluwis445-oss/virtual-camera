#include "video_processor.h"
#include <android/log.h>
#include <cstring>
#include <cmath>

#define LOG_TAG "VideoProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

VideoProcessor::VideoProcessor(AAssetManager* assetManager, const std::string& videoPath)
        : videoAsset(nullptr), videoSize(0), currentPosition(0), width(640), height(480),
          frameRate(30), initialized(false), decoding(false) {

    // Check if it's a file path (external video) or asset path
    if (videoPath.length() > 0 && videoPath[0] == '/') {
        // External file path
        FILE* file = fopen(videoPath.c_str(), "rb");
        if (file) {
            fseek(file, 0, SEEK_END);
            videoSize = ftell(file);
            fseek(file, 0, SEEK_SET);
            fclose(file);
            
            LOGD("Loaded external video file: %s, size: %zu", videoPath.c_str(), videoSize);
            initialized = true;
            parseVideoHeader();
        } else {
            LOGD("Failed to open external video file: %s", videoPath.c_str());
        }
    } else if (assetManager) {
        // Asset path
        videoAsset = AAssetManager_open(assetManager, videoPath.c_str(), AASSET_MODE_BUFFER);
        if (videoAsset) {
            videoSize = AAsset_getLength(videoAsset);
            LOGD("Loaded video asset: %s, size: %zu", videoPath.c_str(), videoSize);
            initialized = true;
            parseVideoHeader();
        } else {
            LOGD("Failed to open video asset: %s", videoPath.c_str());
        }
    }
}

VideoProcessor::~VideoProcessor() {
    stopDecoding();
    if (videoAsset) {
        AAsset_close(videoAsset);
    }
}

void VideoProcessor::parseVideoHeader() {
    // For demo purposes, we'll use test patterns
    // In a real implementation, you would parse actual video file headers
    // using libraries like FFmpeg or MediaCodec
    
    if (videoAsset && videoSize > 0) {
        // Check if it's an MP4 file by looking at the file signature
        const void* data = AAsset_getBuffer(videoAsset);
        if (data) {
            const uint8_t* bytes = static_cast<const uint8_t*>(data);
            // MP4 files start with specific bytes
            if (videoSize >= 8 && 
                bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p') {
                LOGD("Detected MP4 video file");
            }
        }
    }
    
    // Set default parameters for demo
    width = 640;
    height = 480;
    frameRate = 30;
    LOGD("Video parameters: %dx%d, %d fps", width, height, frameRate);
}

std::vector<uint8_t> VideoProcessor::getNextFrame() {
    if (!initialized) {
        // Return a default black frame if not initialized
        return std::vector<uint8_t>(width * height * 3, 0);
    }

    // Try to get a decoded frame from the queue
    std::lock_guard<std::mutex> lock(frameMutex);
    if (!frameQueue.empty()) {
        std::vector<uint8_t> frame = frameQueue.front();
        frameQueue.pop();
        return frame;
    }
    
    // If no decoded frames available, generate a test frame
    static int frameCounter = 0;
    return generateTestFrame(frameCounter++);
}

std::vector<uint8_t> VideoProcessor::generateTestFrame(int frameNumber) {
    std::vector<uint8_t> frame(width * height * 3);

    // Generate a simple test pattern that changes over time
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int idx = (y * width + x) * 3;

            // Create a moving pattern
            float fx = static_cast<float>(x) / width;
            float fy = static_cast<float>(y) / height;
            float time = frameNumber / 30.0f; // 30 fps

            // RGB values based on position and time
            frame[idx] = static_cast<uint8_t>(255 * (0.5 + 0.5 * sin(fx * 10 + time)));     // R
            frame[idx + 1] = static_cast<uint8_t>(255 * (0.5 + 0.5 * sin(fy * 10 + time))); // G
            frame[idx + 2] = static_cast<uint8_t>(255 * (0.5 + 0.5 * sin((fx + fy) * 5 + time))); // B
        }
    }

    return frame;
}

int VideoProcessor::getWidth() const {
    return width;
}

int VideoProcessor::getHeight() const {
    return height;
}

int VideoProcessor::getFrameRate() const {
    return frameRate;
}

bool VideoProcessor::isInitialized() const {
    return initialized;
}

void VideoProcessor::startDecoding() {
    if (decoding || !initialized) {
        return;
    }
    
    decoding = true;
    decodeThread = std::thread(&VideoProcessor::decodeVideoFrames, this);
    LOGD("Video decoding started");
}

void VideoProcessor::stopDecoding() {
    if (!decoding) {
        return;
    }
    
    decoding = false;
    if (decodeThread.joinable()) {
        decodeThread.join();
    }
    LOGD("Video decoding stopped");
}

void VideoProcessor::decodeVideoFrames() {
    LOGD("Starting video frame decoding thread");
    
    while (decoding) {
        try {
            std::vector<uint8_t> frame = decodeFrameFromFile();
            if (!frame.empty()) {
                std::lock_guard<std::mutex> lock(frameMutex);
                frameQueue.push(frame);
                
                // Limit queue size to prevent memory issues
                if (frameQueue.size() > 10) {
                    frameQueue.pop();
                }
            }
            
            // Maintain frame rate
            std::this_thread::sleep_for(std::chrono::milliseconds(1000 / frameRate));
        } catch (const std::exception& e) {
            LOGE("Error in video decoding: %s", e.what());
        }
    }
    
    LOGD("Video frame decoding thread stopped");
}

std::vector<uint8_t> VideoProcessor::decodeFrameFromFile() {
    // In a real implementation, you would use FFmpeg or MediaCodec here
    // For now, we'll generate test frames
    static int frameCounter = 0;
    return generateTestFrame(frameCounter++);
}