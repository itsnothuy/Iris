#!/bin/bash

# Iris MVP - Quick Testing Script for Android Simulator

set -e

echo "🧪 Testing Iris MVP on Android Simulator..."

# Set up Android SDK paths
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

# Check if emulator is connected
echo "📱 Checking emulator connection..."
DEVICES=$(adb devices | grep -c "device$")
if [ $DEVICES -eq 0 ]; then
    echo "❌ No Android device/emulator connected"
    echo "Please start the emulator first:"
    echo "$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_36.0 &"
    exit 1
fi
echo "✅ Emulator connected"

# Check if app is installed
echo "📦 Checking if Iris MVP is installed..."
if adb shell pm list packages | grep -q "com.nervesparks.iris"; then
    echo "✅ Iris MVP is installed"
else
    echo "❌ Iris MVP not installed. Installing now..."
    adb install app/build/outputs/apk/debug/app-debug.apk
    echo "✅ Installation complete"
fi

# Launch the app
echo "🚀 Launching Iris MVP..."
adb shell am start -n com.nervesparks.iris/.MainActivity
sleep 3

# Check if app is running
if adb shell "ps | grep iris" > /dev/null; then
    echo "✅ Iris MVP is running successfully"
    PROCESS_ID=$(adb shell "ps | grep iris" | awk '{print $2}')
    echo "   Process ID: $PROCESS_ID"
else
    echo "❌ App failed to start"
    exit 1
fi

# Check app logs for errors
echo "📋 Checking for startup errors..."
adb logcat -d | grep -E "(FATAL|ERROR)" | grep iris || echo "   No critical errors found"

# App information
echo "📊 App Information:"
echo "   Package: com.nervesparks.iris"
echo "   Main Activity: .MainActivity"
echo "   APK Size: $(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')"

# Test suggestions
echo ""
echo "🎯 Manual Testing Suggestions:"
echo "1. Test chat interface - send a message"
echo "2. Verify mock AI response appears"
echo "3. Check settings and parameter controls"
echo "4. Create new conversation"
echo "5. Test app persistence (close/reopen)"

# Screenshots
echo ""
echo "📸 To capture screenshots:"
echo "adb shell screencap /sdcard/screenshot.png"
echo "adb pull /sdcard/screenshot.png ."

echo ""
echo "🎉 Iris MVP is ready for testing on Android simulator!"
echo "   Open the emulator window to interact with the app"