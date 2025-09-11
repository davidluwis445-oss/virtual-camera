@echo off
echo Building Virtual Camera App...
echo.

REM Clean previous build
echo Cleaning previous build...
call gradlew clean

REM Build the project
echo Building project...
call gradlew assembleDebug

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

REM Install on connected device
echo Installing on device...
call gradlew installDebug

if %ERRORLEVEL% neq 0 (
    echo Installation failed! Make sure a device is connected and USB debugging is enabled.
    pause
    exit /b 1
)

echo.
echo Build and installation completed successfully!
echo You can now run the app on your device.
pause
