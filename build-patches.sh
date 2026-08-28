#!/usr/bin/env bash
set -euo pipefail

echo "Building Microg Patches..."
cd microg-patches
chmod +x ./gradlew

./gradlew buildAndroid --no-daemon || {
    echo "Failed to build microg patches."
    exit 1
}

cp -f patches/build/libs/patches-1.0.0.mpp ../patch/microg-patches.mpp || {
    echo "Failed to copy microg-patches.mpp to ../patch/."
    exit 1
}

echo "Microg patches built successfully"
exit 0
