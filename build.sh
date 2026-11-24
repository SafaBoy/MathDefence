#!/usr/bin/env bash
set -euo pipefail

# MathDefence Local APK Build Script
# Generates Debug and/or Release APKs locally using either your host JDK + Android SDK
# or an automatic Docker builder container (Dockerfile.build).
#
# Usage:
#   ./build.sh [options]
#
# Options:
#   --debug       Build debug APK only
#   --release     Build release APK only
#   --all         Build both debug and release APKs (default)
#   --test        Run unit tests before building APKs
#   --clean       Run Gradle clean before building
#   --docker      Force building inside Docker container
#   --help, -h    Show this help message

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

BUILD_DEBUG=true
BUILD_RELEASE=true
RUN_TESTS=false
RUN_CLEAN=false
FORCE_DOCKER=false
DOCKER_IMAGE="mathdefence-android-builder:sdk35"

for arg in "$@"; do
    case "$arg" in
        --debug)
            BUILD_DEBUG=true
            BUILD_RELEASE=false
            ;;
        --release)
            BUILD_DEBUG=false
            BUILD_RELEASE=true
            ;;
        --all)
            BUILD_DEBUG=true
            BUILD_RELEASE=true
            ;;
        --test)
            RUN_TESTS=true
            ;;
        --clean)
            RUN_CLEAN=true
            ;;
        --docker)
            FORCE_DOCKER=true
            ;;
        --help|-h)
            sed -n '3,17p' "$0" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            echo "Unknown option: $arg" >&2
            echo "Run './build.sh --help' for usage." >&2
            exit 1
            ;;
    esac
done

TASKS=()
if [ "$RUN_CLEAN" = true ]; then
    TASKS+=("clean")
fi
if [ "$RUN_TESTS" = true ]; then
    TASKS+=("testDebugUnitTest")
fi
if [ "$BUILD_DEBUG" = true ]; then
    TASKS+=("assembleDebug")
fi
if [ "$BUILD_RELEASE" = true ]; then
    TASKS+=("assembleRelease")
fi

echo "=================================================="
echo "            MathDefence - APK Builder             "
echo "=================================================="
echo "Gradle tasks: ${TASKS[*]}"

HAS_LOCAL_SDK=false
if command -v java >/dev/null 2>&1 && [ -n "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ] && [ -d "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]; then
    HAS_LOCAL_SDK=true
fi

chmod +x ./gradlew

if [ "$HAS_LOCAL_SDK" = true ] && [ "$FORCE_DOCKER" = false ]; then
    echo "Using local JDK + Android SDK (${ANDROID_HOME:-$ANDROID_SDK_ROOT})..."
    ./gradlew "${TASKS[@]}" --stacktrace
else
    if ! command -v docker >/dev/null 2>&1; then
        echo "Error: Neither local JDK+Android SDK nor Docker was found." >&2
        echo "Install JDK 17 + Android SDK (set ANDROID_HOME) or install Docker." >&2
        exit 1
    fi

    if ! docker image inspect "$DOCKER_IMAGE" >/dev/null 2>&1; then
        echo "Building Docker Android SDK image ($DOCKER_IMAGE) [one-time setup]..."
        docker build -f Dockerfile.build -t "$DOCKER_IMAGE" .
    fi

    echo "Building inside Docker container ($DOCKER_IMAGE)..."
    GRADLE_CACHE_DIR="${HOME}/.cache/mathdefence-gradle"
    mkdir -p "$GRADLE_CACHE_DIR"
    docker run --rm \
        -u "$(id -u):$(id -g)" \
        -e HOME=/gradle-cache \
        -e GRADLE_USER_HOME=/gradle-cache \
        -e ANDROID_USER_HOME=/gradle-cache/.android \
        -e GRADLE_OPTS="-Duser.home=/gradle-cache" \
        -v "$SCRIPT_DIR":/workspace \
        -v "$GRADLE_CACHE_DIR":/gradle-cache \
        -w /workspace \
        "$DOCKER_IMAGE" \
        ./gradlew "${TASKS[@]}" --no-daemon --stacktrace
fi

mkdir -p dist

if [ "$BUILD_DEBUG" = true ]; then
    DEBUG_APK=$(find app/build/outputs/apk/debug -name "*.apk" | head -n 1 || true)
    if [ -n "$DEBUG_APK" ] && [ -f "$DEBUG_APK" ]; then
        cp "$DEBUG_APK" dist/MathDefence-debug.apk
        echo "Copied: dist/MathDefence-debug.apk"
    fi
fi

if [ "$BUILD_RELEASE" = true ]; then
    RELEASE_APK=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1 || true)
    if [ -n "$RELEASE_APK" ] && [ -f "$RELEASE_APK" ]; then
        cp "$RELEASE_APK" dist/MathDefence-release.apk
        echo "Copied: dist/MathDefence-release.apk"
    fi
fi

echo "=================================================="
echo "Build completed! Output APKs in ./dist:"
ls -lh dist/*.apk 2>/dev/null || echo "(No APKs found in dist/)"
echo "=================================================="
