#ifndef VIRTUALCAMERA_VIDEO_PROCESSOR_H
#define VIRTUALCAMERA_VIDEO_PROCESSOR_H

#include <vector>
#include <string>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <thread>
#include <mutex>
#include <queue>

class VideoProcessor {
public:
    VideoProcessor(AAssetManager* assetManager, const std::string& videoPath);
    ~VideoProcessor();

    std::vector<uint8_t> getNextFrame();
    int getWidth() const;
    int getHeight() const;
    int getFrameRate() const;
    bool isInitialized() const;
    void startDecoding();
    void stopDecoding();

private:
    AAsset* videoAsset;
    size_t videoSize;
    size_t currentPosition;
    int width;
    int height;
    int frameRate;
    bool initialized;
    bool decoding;
    
    // Video decoding thread
    std::thread decodeThread;
    std::mutex frameMutex;
    std::queue<std::vector<uint8_t>> frameQueue;
    
    void parseVideoHeader();
    std::vector<uint8_t> generateTestFrame(int frameNumber);
    void decodeVideoFrames();
    std::vector<uint8_t> decodeFrameFromFile();
};

#endif // VIRTUALCAMERA_VIDEO_PROCESSOR_H