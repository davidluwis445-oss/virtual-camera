#include "video_processor.h"
#include <android/log.h>
#include <cstring>
#include <cmath>

#define LOG_TAG "VideoProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

VideoProcessor::VideoProcessor(AAssetManager* assetManager, const std::string& videoPath)
        : videoAsset(nullptr), videoSize(0), currentPosition(0), width(640), height(480),
          frameRate(30), initialized(false) {

    if (assetManager) {
        videoAsset = AAssetManager_open(assetManager, videoPath.c_str(), AASSET_MODE_BUFFER);
        if (videoAsset) {
            videoSize = AAsset_getLength(videoAsset);
            LOGD("Loaded video asset: %s, size: %zu", videoPath.c_str(), videoSize);
            initialized = true;

            // For demo purposes, we'll use test patterns
            // In a real implementation, you would parse the video file here
            parseVideoHeader();
        } else {
            LOGD("Failed to open video asset: %s", videoPath.c_str());
        }
    }
}

VideoProcessor::~VideoProcessor() {
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

    // For demo purposes, generate a test pattern
    // In a real implementation, you would decode the actual video frames
    static int frameCounter = 0;
    std::vector<uint8_t> frame = generateTestFrame(frameCounter++);

    // Simulate video playback by seeking in the asset
    if (videoAsset) {
        currentPosition = (currentPosition + 100) % videoSize; // Simplified
        AAsset_seek(videoAsset, currentPosition, SEEK_SET);
    }

    return frame;
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