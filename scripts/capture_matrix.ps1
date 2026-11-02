$ErrorActionPreference = "Stop"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

function Ensure-Dir($path) {
    if (-not (Test-Path $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
    }
}

function Capture-Screen($destPath) {
    & $adb shell screencap -p /sdcard/capture.png
    & $adb pull /sdcard/capture.png $destPath | Out-Null
    $bytes = [System.IO.File]::ReadAllBytes($destPath)
    if ($bytes[0] -ne 137 -or $bytes[1] -ne 80) {
        throw "Captured PNG $destPath is corrupted or invalid!"
    }
    Write-Host "Captured: $destPath ($($bytes.Length) bytes, valid PNG)"
}

function Tap($x, $y, $sleepMs = 800) {
    & $adb shell input tap $x $y
    Start-Sleep -Milliseconds $sleepMs
}

function BackKey($sleepMs = 800) {
    & $adb shell input keyevent 4
    Start-Sleep -Milliseconds $sleepMs
}

# Ensure directories
@("light", "dark", "amoled", "glass") | ForEach-Object {
    Ensure-Dir "qa_screenshots/$_"
}

# Helper to open Themes screen from anywhere
function Open-ThemesScreen {
    # Press back a couple times to ensure we are at Home or close any sheets
    BackKey 500
    BackKey 500
    BackKey 500
    # Tap Home
    Tap 140 2232 800
    # Tap Profile avatar
    Tap 977 224 800
    # Tap Settings
    Tap 1007 217 800
    # Tap Themes & Live Backgrounds
    Tap 383 511 1000
}

# Capture all 8 screens for the currently active theme
function Capture-AllScreensForCurrentTheme($themeName) {
    Write-Host "================ Capturing $themeName ================"
    # We are currently in Themes screen
    Capture-Screen "qa_screenshots/$themeName/themes.png"
    
    # Back to Settings
    Tap 74 222 800
    Capture-Screen "qa_screenshots/$themeName/settings.png"
    
    # Back to Profile
    Tap 74 222 800
    Capture-Screen "qa_screenshots/$themeName/profile.png"
    
    # Back to Home
    Tap 74 222 800
    Capture-Screen "qa_screenshots/$themeName/home.png"
    
    # Insights tab
    Tap 344 2232 1000
    Capture-Screen "qa_screenshots/$themeName/insights.png"
    
    # Lend & Borrow tab
    Tap 548 2232 1000
    Capture-Screen "qa_screenshots/$themeName/lend_borrow.png"
    
    # Goals tab
    Tap 752 2232 1000
    Capture-Screen "qa_screenshots/$themeName/goals.png"
    
    # Return to Home tab
    Tap 140 2232 800
    
    # Add Transaction sheet
    Tap 970 2232 1200
    Capture-Screen "qa_screenshots/$themeName/add_transaction.png"
    
    # Dismiss Add Transaction sheet
    BackKey 800
    Write-Host "Finished capturing $themeName"
}

# 1. LIGHT THEME (already set to Light)
Capture-AllScreensForCurrentTheme "light"

# 2. DARK THEME
Open-ThemesScreen
# Tap Dark (446, 987)
Tap 446 987 1200
Capture-AllScreensForCurrentTheme "dark"

# 3. AMOLED THEME
Open-ThemesScreen
# Tap AMOLED (724, 987)
Tap 724 987 1200
Capture-AllScreensForCurrentTheme "amoled"

# 4. GLASS THEME
Open-ThemesScreen
# Scroll the Theme Mode row slightly to the left, or tap the first live shader preset at (500, 1250)
Tap 500 1250 1500
Capture-AllScreensForCurrentTheme "glass"

Write-Host "ALL THEMES AND SCREENS CAPTURED SUCCESSFULLY!"

