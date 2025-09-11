#ifndef VIRTUALCAMERA_VIDEO_PROCESSOR_H
#define VIRTUALCAMERA_VIDEO_PROCESSOR_H

#include <vector>
#include <string>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

class VideoProcessor {
public:
    VideoProcessor(AAssetManager* assetManager, const std::string& videoPath);
    ~VideoProcessor();

    std::vector<uint8_t> getNextFrame();
    int getWidth() const;
    int getHeight() const;
    int getFrameRate() const;
    bool isInitialized() const;

private:
    AAsset* videoAsset;
    size_t videoSize;
    size_t currentPosition;
    int width;
    int height;
    int frameRate;
    bool initialized;

    void parseVideoHeader();
    std::vector<uint8_t> generateTestFrame(int frameNumber);
};

#endif // VIRTUALCAMERA_VIDEO_PROCESSOR_H