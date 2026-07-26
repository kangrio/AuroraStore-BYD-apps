#!/usr/bin/env bash
set -euo pipefail

mkdir -p apps

APKMD_RELEASE_URL="https://github.com/tanishqmanuja/apkmirror-downloader/releases/latest/download/apkmd"
apps=(
  "google-inc|youtube|com.google.android.youtube|Youtube"
  "google-inc|youtube-music|com.google.android.apps.youtube.music|YTMusic"
)

echo "Downloading apkmd..."
curl -fsSL "$APKMD_RELEASE_URL" -o patch/apkmd
chmod +x patch/apkmd

for app_info in "${apps[@]}"; do
    IFS='|' read -r org app package name <<< "$app_info"

    latest_version=$(
      java -jar patch/morphe-cli.jar list-versions \
      --patches=patch/patches.mpp \
      -f "$package" | 
      grep -E '^[[:space:]]*[0-9]' | 
      awk '{print $1}' | 
      sort -V | 
      tail -n1
    )

    echo "Downloading $org/$app ($latest_version)..."

    ./patch/apkmd download $org $app \
    -v ${latest_version} \
    -t apk \
    -a universal \
    --fallbackarch arm64-v8a \
    -o apps/${name}_${latest_version}.apk 2>&1 | grep -qi "error:" && { echo "Failed to download $org/$app ($latest_version)."; exit 1; } 
done