cmake_minimum_required(VERSION 3.22.1)

# Set the project name
project("virtualcamera")

# Enable C++ and set standards
enable_language(CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Add your source files
add_library(virtualcamera SHARED
        native-lib.cpp
        video_processor.cpp
)

# Add camera hook library
add_library(camera_hook SHARED
        camera_hook.cpp
        advanced_hook.cpp
        video_processor.cpp
)

# Add Camera2 hook library
add_library(camera2_hook SHARED
        camera2_hook.cpp
        video_processor.cpp
)

# Find required libraries
find_library(android-lib android)
find_library(log-lib log)

# 🔥 CRITICAL: Link the C++ standard library
target_link_libraries(virtualcamera
        ${android-lib}
        ${log-lib}
        c++_shared  # This fixes the 'new' operator error
)

# Link camera hook library
target_link_libraries(camera_hook
        ${android-lib}
        ${log-lib}
        c++_shared
        dl  # For dynamic library loading
)

# Link Camera2 hook library
target_link_libraries(camera2_hook
        ${android-lib}
        ${log-lib}
        c++_shared
        dl
)

# Add include directories if needed
include_directories(src/main/cpp)