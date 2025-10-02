cmake_minimum_required(VERSION 3.22.1)

# Set the project name
project("virtualcamera")

# Enable C++ and set standards
enable_language(CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Add simple camera replacement library (WORKING SOLUTION)
add_library(simple_camera_replacement SHARED
        simple_camera_replacement.cpp
)

# Add real camera hook library (PARTIAL WORKING)
add_library(real_camera_hook SHARED
        real_camera_hook.cpp
)

# Add improved real camera hook library (FIXED VERSION)
add_library(improved_real_camera_hook SHARED
        improved_real_camera_hook.cpp
        plt_hook.cpp
)

# Add system wide camera hook library (BEST SYSTEM SOLUTION)
add_library(system_wide_camera_hook SHARED
        SystemWideCameraHook.cpp
)

# Find required libraries
find_library(android-lib android)
find_library(log-lib log)

# Link simple camera replacement library
target_link_libraries(simple_camera_replacement
        ${android-lib}
        ${log-lib}
        c++_shared
        dl
)

# Link real camera hook library
target_link_libraries(real_camera_hook
        ${android-lib}
        ${log-lib}
        c++_shared
        dl
)

# Link improved real camera hook library
target_link_libraries(improved_real_camera_hook
        ${android-lib}
        ${log-lib}
        c++_shared
        dl
)

# Link system wide camera hook library
target_link_libraries(system_wide_camera_hook
        ${android-lib}
        ${log-lib}
        c++_shared
        dl
)

# Add include directories if needed
include_directories(src/main/cpp)
