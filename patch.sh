#!/usr/bin/env bash
set -euo pipefail

if [[ -f patch-version.txt ]]; then
    CURRENT_PATCH_VERSION=$(<patch-version.txt)
else
    CURRENT_PATCH_VERSION="0.0.0"
fi

CLI_RELEASE_URL="https://api.github.com/repos/MorpheApp/morphe-desktop/releases/latest"
BUNDLE_RELEASE_URL="https://api.github.com/repos/MorpheApp/morphe-patches/releases/latest"

mkdir -p patch
mkdir -p out
trap 'rm -rf patch apps out' EXIT

echo "Fetching latest Morphe CLI..."
CLI_URL=$(curl -fsSL "$CLI_RELEASE_URL" | jq -r '.assets[0].browser_download_url')

echo "Fetching patch metadata..."
BUNDLE_RELEASE_JSON=$(curl -fsSL "$BUNDLE_RELEASE_URL")

LATEST_PATCH_VERSION=$(echo "$BUNDLE_RELEASE_JSON" | jq -r '.tag_name' | sed 's/^v//')
PATCH_URL="https://github.com/MorpheApp/morphe-patches/releases/download/v${LATEST_PATCH_VERSION}/patches-${LATEST_PATCH_VERSION}.mpp"

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

    chmod +x downloads.sh
   ./downloads.sh || { echo "Failed to download APKs. Exiting."; exit 1; }

    chmod +x build-patches.sh
   ./build-patches.sh || { echo "Failed to build MicroG patches."; exit 1; }

    shopt -s nullglob

    # Collect all APKs
    apks=(apps/*.apk)

    if [ ${#apks[@]} -eq 0 ]; then
        echo "No APK files found in apps/"
        exit 1
    fi

    echo "$LATEST_PATCH_VERSION" > patch-version.txt

    for apk in "${apks[@]}"; do
        echo "========================================"
        echo "Patching: $apk"

        apk_name="${apk##*/}"
        apk_name="${apk_name%.apk}"
        output="${apk_name}-morphe-${LATEST_PATCH_VERSION}.apk"

        java -jar patch/morphe-cli.jar patch \
            -p patch/microg-patches.mpp \
            -e "MicroG Support" \
            -p patch/patches.mpp \
            -d "GmsCore support" \
            -d "Custom branding" \
            --keystore="Morphe.keystore" \
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
            patch/patches.mpp \
            --title "$TITLE" \
            --notes-file release_notes.md

        rm -f release_notes.md
        
        echo "$LATEST_PATCH_VERSION" > patch-version.txt

        if ! git diff --quiet patch-version.txt; then
            git config user.name "github-actions"
            git config user.email "github-actions@github.com"

            git add patch-version.txt
            git commit -m "Update patch version to $LATEST_PATCH_VERSION"
            git push
        fi
    fi

    echo "========================================"
    echo "All APKs patched successfully."
    echo "Updated to patch version $LATEST_PATCH_VERSION"
else
    echo "Already up to date version $CURRENT_PATCH_VERSION"
fi
