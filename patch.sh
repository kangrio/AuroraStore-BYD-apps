#!/usr/bin/env bash
set -euo pipefail

# GitHub Actions repository variable:
# env:
#   CURRENT_PATCH_VERSION: ${{ vars.PATCH_VERSION }}

CURRENT_PATCH_VERSION="${CURRENT_PATCH_VERSION:-0.0.0}"

CLI_RELEASE_URL="https://api.github.com/repos/MorpheApp/morphe-desktop/releases/latest"
BUNDLE_JSON_URL="https://raw.githubusercontent.com/MorpheApp/morphe-patches/main/patches-bundle.json"

mkdir -p patch
mkdir -p out
trap 'rm -rf patch' EXIT

echo "Fetching latest Morphe CLI..."
CLI_URL=$(curl -fsSL "$CLI_RELEASE_URL" | jq -r '.assets[0].browser_download_url')

echo "Fetching patch metadata..."
BUNDLE_JSON=$(curl -fsSL "$BUNDLE_JSON_URL")

LATEST_PATCH_VERSION=$(echo "$BUNDLE_JSON" | jq -r '.version')
PATCH_URL=$(echo "$BUNDLE_JSON" | jq -r '.download_url')

echo "Current patch version: $CURRENT_PATCH_VERSION"
echo "Latest  patch version: $LATEST_PATCH_VERSION"

version_gt() {
    [ "$(printf '%s\n%s\n' "$2" "$1" | sort -V | tail -n1)" = "$1" ] &&
    [ "$1" != "$2" ]
}

if [ -z "$CURRENT_PATCH_VERSION" ] || \
   version_gt "$LATEST_PATCH_VERSION" "$CURRENT_PATCH_VERSION"; then

    if [ ! -f patch/morphe-cli.jar ]; then
        echo "Downloading Morphe CLI..."
        curl -fsSL "$CLI_URL" -o patch/morphe-cli.jar
    fi

    echo "Downloading patches..."
    curl -fsSL "$PATCH_URL" -o patch/patches.mpp

    shopt -s nullglob

    # Merge split APKs (.apk.001, .apk.002, ...)
    for first in apps/*.apk.001; do
        [ -e "$first" ] || continue

        base="${first%.001}"
        echo "Merging $(basename "$base")..."

        rm -f "$base"

        cat "${base}".* > "$base"
    done

    # Collect all APKs
    apks=(apps/*.apk)

    if [ ${#apks[@]} -eq 0 ]; then
        echo "No APK files found in apps/"
        exit 1
    fi

    for apk in "${apks[@]}"; do
        echo "========================================"
        echo "Patching: $apk"

        apk_name="${apk##*/}"
        apk_name="${apk_name%.apk}"
        output="${apk_name}-morphe-${LATEST_PATCH_VERSION}.apk"

        java -jar patch/morphe-cli.jar patch \
            -p microg-patches.mpp \
            -p patch/patches.mpp \
            -d "GmsCore support" \
            -d "Custom branding" \
            -o "out/$output" \
            "$apk"

        echo "Finished patching: $apk"
    done

    # Create GitHub Release
    if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
        echo "Creating GitHub Release..."

        TAG="v${LATEST_PATCH_VERSION}"
        TITLE="Morphe Patch ${LATEST_PATCH_VERSION}"

        cat > release_notes.md <<EOF
## Morphe Patch Release

**patch_version:** ${LATEST_PATCH_VERSION}

### Details
- Updated Morphe patches to ${LATEST_PATCH_VERSION}
- Generated automatically by GitHub Actions.
EOF

        # Delete existing release/tag if it already exists
        if gh release view "$TAG" >/dev/null 2>&1; then
            echo "Release $TAG already exists, replacing..."
            gh release delete "$TAG" --yes
            git push origin ":refs/tags/$TAG" || true
        fi

        gh release create "$TAG" \
            out/* \
            --title "$TITLE" \
            --notes-file release_notes.md

        rm -f release_notes.md
        gh variable set PATCH_VERSION --body "$LATEST_PATCH_VERSION"
    fi

    echo "========================================"
    echo "All APKs patched successfully."
    echo "Updated to patch version $LATEST_PATCH_VERSION"
else
    echo "Already up to date version $CURRENT_PATCH_VERSION"
fi