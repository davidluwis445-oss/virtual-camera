cmake_minimum_required(VERSION 3.22.1)

# Set the project name
project("virtualcamera")

# Enable C++ and set standards
enable_language(CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Add system camera hook library (main virtual camera functionality)
add_library(system_camera_hook SHARED
        system_camera_hook.cpp
        video_processor.cpp
        plt_hook.cpp
)

# Find required libraries
find_library(android-lib android)
find_library(log-lib log)

# Link system camera hook library
target_link_libraries(system_camera_hook
        ${android-lib}
        ${log-lib}
        c++_shared
        dl
)

# Add include directories if needed
include_directories(src/main/cpp)