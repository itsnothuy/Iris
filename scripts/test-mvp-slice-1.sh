#!/bin/bash
# Test Execution Script for MVP Slice 1
# Run this script once network access to dl.google.com is restored

set -e  # Exit on error

echo "=========================================="
echo "MVP Slice 1 - Test Execution Script"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    print_error "Must be run from the project root directory"
    exit 1
fi

print_info "Cleaning previous build artifacts..."
./gradlew clean

echo ""
print_info "Running unit tests for Message data class..."
./gradlew testDebugUnitTest --tests "com.nervesparks.iris.data.MessageTest" || {
    print_error "Unit tests failed!"
    exit 1
}
print_success "Unit tests passed!"

echo ""
print_info "Building debug APK..."
./gradlew assembleDebug || {
    print_error "Build failed!"
    exit 1
}
print_success "Build succeeded!"

echo ""
print_info "Note: UI tests require a connected device or emulator"
read -p "Do you have a device/emulator connected? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_info "Running Compose UI tests..."
    ./gradlew connectedDebugAndroidTest \
        --tests "com.nervesparks.iris.ui.components.MessageBubbleTest" \
        --tests "com.nervesparks.iris.ui.components.ProcessingIndicatorTest" || {
        print_error "UI tests failed!"
        exit 1
    }
    print_success "UI tests passed!"
else
    print_info "Skipping UI tests"
fi

echo ""
print_info "Generating test reports..."
./gradlew testDebugUnitTest jacocoTestReport 2>/dev/null || print_info "Coverage report generation skipped"

echo ""
echo "=========================================="
print_success "All tests completed successfully!"
echo "=========================================="
echo ""
echo "Test reports can be found at:"
echo "  - Unit tests: app/build/reports/tests/testDebugUnitTest/index.html"
echo "  - UI tests: app/build/reports/androidTests/connected/index.html"
echo "  - Coverage: app/build/reports/jacoco/jacocoTestReport/html/index.html"
echo ""
