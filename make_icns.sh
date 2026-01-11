#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 input.png output.icns"
  exit 1
fi

INPUT="$1"
OUTPUT="$2"

# Step 1: figure out width/height
WIDTH=$(sips -g pixelWidth "$INPUT" | awk '/pixelWidth:/ {print $2}')
HEIGHT=$(sips -g pixelHeight "$INPUT" | awk '/pixelHeight:/ {print $2}')

# Step 2: pick the larger dimension as the square size
SIZE=$(( WIDTH > HEIGHT ? WIDTH : HEIGHT ))

# Step 3: create a square padded PNG with transparent background
PADDED="$(mktemp -d)/padded.png"
sips -s format png \
     -s formatOptions best \
     --padToHeightWidth $SIZE $SIZE \
     "$INPUT" --out "$PADDED" >/dev/null

# Step 4: generate .iconset folder
TMPDIR="$(mktemp -d)/icon.iconset"
mkdir -p "$TMPDIR"

sips -z 16 16     "$PADDED" --out "$TMPDIR/icon_16x16.png"
sips -z 32 32     "$PADDED" --out "$TMPDIR/icon_16x16@2x.png"
sips -z 32 32     "$PADDED" --out "$TMPDIR/icon_32x32.png"
sips -z 64 64     "$PADDED" --out "$TMPDIR/icon_32x32@2x.png"
sips -z 128 128   "$PADDED" --out "$TMPDIR/icon_128x128.png"
sips -z 256 256   "$PADDED" --out "$TMPDIR/icon_128x128@2x.png"
sips -z 256 256   "$PADDED" --out "$TMPDIR/icon_256x256.png"
sips -z 512 512   "$PADDED" --out "$TMPDIR/icon_256x256@2x.png"
sips -z 512 512   "$PADDED" --out "$TMPDIR/icon_512x512.png"
sips -z 1024 1024 "$PADDED" --out "$TMPDIR/icon_512x512@2x.png"

# Step 5: make .icns
iconutil -c icns "$TMPDIR" -o "$OUTPUT"

echo "✅ Created $OUTPUT"
