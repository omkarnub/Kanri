@echo off
title Launching Kanri Pixel 8 Emulator
echo Starting Pixel 8 Emulator...
start "" "%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd Pixel_8
echo Waiting for device to boot...
timeout /t 8 /nobreak >nul
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" wait-for-device
echo Launching Kanri app...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" shell am start -n com.omkarnub.kanri/.MainActivity
echo Done! Enjoy exploring Kanri!
timeout /t 3 /nobreak >nul
