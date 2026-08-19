#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"
SDK_ROOT="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
CMDLINE_TOOLS="$SDK_ROOT/cmdline-tools/latest"
APK_PATH="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/platform-tools:$CMDLINE_TOOLS/bin:$PATH"

if [[ "$(uname -s)" == "Darwin" ]] && command -v brew >/dev/null 2>&1 && brew list --formula openjdk@17 >/dev/null 2>&1; then
  export JAVA_HOME="${JAVA_HOME:-$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home}"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ ! -x "$ANDROID_DIR/gradlew" ]]; then
  echo "Missing android/gradlew." >&2
  exit 1
fi

if [[ ! -f "$ANDROID_DIR/local.properties" ]]; then
  echo "sdk.dir=$SDK_ROOT" > "$ANDROID_DIR/local.properties"
fi

echo "==> Building Work Tracker debug APK..."
cd "$ANDROID_DIR"
./gradlew assembleDebug --no-daemon

if [[ ! -f "$APK_PATH" ]]; then
  echo "Build finished but APK not found at $APK_PATH" >&2
  exit 1
fi

APK_SIZE="$(du -h "$APK_PATH" | awk '{print $1}')"
cat <<EOF

Build complete.

APK: $APK_PATH
Size: $APK_SIZE

Install on phone:
  1. Copy app-debug.apk to Android
  2. Open file and install

EOF
