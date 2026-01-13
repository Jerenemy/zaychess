#!/usr/bin/env bash
set -euo pipefail

# build jar
# from repo root
rm -rf out/
mkdir -p out/classes out/artifacts

find Chess/src -name "*.java" > /tmp/sources.txt
javac -d out/classes @/tmp/sources.txt

# copy resources into the class output
rsync -a Chess/src/com/jeremyzay/zaychess/view/assets \
  out/classes/com/jeremyzay/zaychess/view/

# build the app jar with the correct Main-Class
jar --create \
  --file out/artifacts/Zaychess.jar \
  --manifest Chess/src/META-INF/MANIFEST.MF \
  -C out/classes .

rm -rf dist



# resolve paths relative to this script
SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

APPNAME="Zaychess"
MAIN_CLASS="com.jeremyzay.zaychess.App"
BUNDLE_ID="com.jeremyzay.zaychess"
ARTIFACTS_DIR="$SCRIPT_DIR/out/artifacts"
DEST="$SCRIPT_DIR/dist"
STAGE="$SCRIPT_DIR/build/macos-resources"
INPUT_DIR="$SCRIPT_DIR/build/jpackage-input"

APP="$DEST/$APPNAME.app"
SIGN_ID="5C8AF79D5FA7AE13418BDE021625DFF59019AAA8"
ENT="sandbox.plist"
APP_ENT="/tmp/${APPNAME}-app-entitlements.plist"
PROFILE="$PWD/$APPNAME.provisionprofile"

# 0) pick a JDK with jpackage (21 preferred, fallback 17)
JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home -v 17)"
JPACKAGE="$JAVA_HOME/bin/jpackage"
if [[ ! -x "$JPACKAGE" ]]; then
  echo "jpackage not found; install JDK 17+."; exit 1
fi

# 1) choose the correct app jar
if [[ -n "${APP_JAR:-}" ]]; then
  JAR_PATH="$APP_JAR"
else
  JAR_PATH="$(find "$ARTIFACTS_DIR" -type f -name '*.jar' \
      ! -name 'Serendipity.jar' \
      ! -path '*/engines/*' \
      ! -path '*/macos-resources/*' \
      -print0 | xargs -0 ls -t 2>/dev/null | head -n 1 || true)"
fi

if [[ -z "${JAR_PATH}" || ! -f "${JAR_PATH}" ]]; then
  echo "No app JAR found in $ARTIFACTS_DIR."
  exit 1
fi

if ! unzip -p "$JAR_PATH" META-INF/MANIFEST.MF | grep -q '^Main-Class:' ; then
  echo "Selected JAR lacks Main-Class."
  exit 1
fi

JAR_DIR="$(dirname "$JAR_PATH")"
MAIN_JAR="$(basename "$JAR_PATH")"
echo "Using app JAR: $MAIN_JAR"

# 2) stage the UCI engine
rm -rf "$STAGE"
mkdir -p "$STAGE/engines"

# Create proper high-res icon (512pt @2x = 1024px)
./make_icns.sh "icon.png" "$STAGE/Zaychess.icns"

ENGINE_SRC=""
for CAND in \
  "$SCRIPT_DIR/Chess/engines/Serendipity.jar" \
  "$SCRIPT_DIR/engines/Serendipity.jar"
do
  if [[ -f "$CAND" ]]; then ENGINE_SRC="$CAND"; break; fi
done
if [[ -z "$ENGINE_SRC" ]]; then
  echo "Serendipity.jar not found."
  exit 1
fi
xattr -d com.apple.quarantine "$ENGINE_SRC" 2>/dev/null || true
cp "$ENGINE_SRC" "$STAGE/engines/Serendipity.jar"

# 2b) stage jpackage input (main app jar + engine jar)
rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR/engines"
cp -f "$JAR_PATH" "$INPUT_DIR/$MAIN_JAR"
cp -f "$ENGINE_SRC" "$INPUT_DIR/engines/Serendipity.jar"

# 2c) build app entitlements with application-identifier for TestFlight
[[ -f "$ENT" ]]      || { echo "Entitlements not found: $ENT"; exit 1; }
[[ -f "$PROFILE" ]]  || { echo "Provisioning profile not found: $PROFILE"; exit 1; }

TEAM_ID="${TEAM_ID:-}"
if [[ -z "$TEAM_ID" ]]; then
  PROFILE_PLIST="$(/usr/bin/mktemp "/tmp/${APPNAME}-profile.XXXXXX.plist")"
  /usr/bin/security cms -D -i "$PROFILE" > "$PROFILE_PLIST"
  TEAM_ID="$(/usr/libexec/PlistBuddy -c "Print :TeamIdentifier:0" "$PROFILE_PLIST" 2>/dev/null || true)"
  rm -f "$PROFILE_PLIST"
fi
if [[ -z "$TEAM_ID" || ! "$TEAM_ID" =~ ^[A-Z0-9]{10}$ ]]; then
  echo "Team ID not found in provisioning profile. Set TEAM_ID env var."
  exit 1
fi
echo "Using Team ID: $TEAM_ID"

cp -f "$ENT" "$APP_ENT"
/usr/libexec/PlistBuddy -c "Add :com.apple.application-identifier string ${TEAM_ID}.${BUNDLE_ID}" "$APP_ENT" 2>/dev/null || \
  /usr/libexec/PlistBuddy -c "Set :com.apple.application-identifier ${TEAM_ID}.${BUNDLE_ID}" "$APP_ENT"
/usr/libexec/PlistBuddy -c "Add :com.apple.developer.team-identifier string ${TEAM_ID}" "$APP_ENT" 2>/dev/null || \
  /usr/libexec/PlistBuddy -c "Set :com.apple.developer.team-identifier ${TEAM_ID}" "$APP_ENT"

# 3) Create a stripped runtime image for Mac App Store
CUSTOM_RUNTIME="$SCRIPT_DIR/build/custom-runtime"
echo "Creating stripped runtime image for Mac App Store..."

# Remove existing custom runtime if it exists
rm -rf "$CUSTOM_RUNTIME"

# Create custom runtime with jlink (strips bin folder and native commands)
"$JAVA_HOME/bin/jlink" \
  --add-modules java.base,java.desktop,java.logging,java.xml,java.naming,java.security.jgss,java.instrument,java.management,java.prefs,jdk.incubator.vector \
  --strip-native-commands \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output "$CUSTOM_RUNTIME"

echo "✅ Custom runtime created at: $CUSTOM_RUNTIME"

# 4) Build the app with custom runtime
jpackage \
  --type app-image \
  --name "$APPNAME" \
  --app-version "1.0.9" \
  --mac-app-category games \
  --input "$INPUT_DIR" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --resource-dir "$STAGE" \
  --dest "$DEST" \
  --runtime-image "$CUSTOM_RUNTIME" \
  --icon "$STAGE/$APPNAME.icns" \
  --java-options "-Dapple.laf.useScreenMenuBar=true" \
  --java-options "--add-modules=jdk.incubator.vector" \
  --mac-package-identifier "$BUNDLE_ID" \
  --mac-app-store \
  --mac-signing-key-user-name "Apple Distribution: Jeremy Theodore Zay (9A9HR6WT4K)" \
  --mac-entitlements "$APP_ENT"

echo "✅ App created: $DEST/$APPNAME.app"

# 5) Explicitly set minimum macOS version to 12.0 to allow ARM64-only
PLIST="$APP/Contents/Info.plist"
echo "Setting minimum macOS version to 12.0..."
/usr/libexec/PlistBuddy -c "Add :LSMinimumSystemVersion string 12.0" "$PLIST" 2>/dev/null || \
/usr/libexec/PlistBuddy -c "Set :LSMinimumSystemVersion 12.0" "$PLIST"

echo "✅ Minimum macOS version set to 12.0 (allows ARM64-only)"

[[ -d "$APP/Contents" ]] || { echo "App not found: $APP"; exit 1; }
[[ -f "$APP_ENT" ]]      || { echo "App entitlements not found: $APP_ENT"; exit 1; }
[[ -f "$PROFILE" ]]      || { echo "Provisioning profile not found: $PROFILE"; exit 1; }

# 6) Embed provisioning profile
xattr -d com.apple.quarantine "$PROFILE" 2>/dev/null || true
cp -fX "$PROFILE" "$APP/Contents/embedded.provisionprofile"

# embed licenses
mkdir -p "$APP/Contents/Resources/ThirdParty"
cp -fX ThirdParty/commons-lang3-LICENSE.txt "$APP/Contents/Resources/ThirdParty/"
cp -fX ThirdParty/commons-lang3-NOTICE.txt  "$APP/Contents/Resources/ThirdParty/"
cp -fX ThirdParty/Serendipity-LICENSE.txt "$APP/Contents/Resources/ThirdParty/"
cp -fX ThirdParty/Serendipity-SOURCE.txt  "$APP/Contents/Resources/ThirdParty/"
cp -fX ThirdParty/NOTICE.txt              "$APP/Contents/Resources/ThirdParty/"
# (optional) strip quarantine
find "$APP/Contents/Resources/ThirdParty" -print0 | xargs -0 xattr -d com.apple.quarantine 2>/dev/null || true


# 7) Create sandbox entitlements for Java runtime executables
cat > /tmp/java_sandbox_entitlements.plist << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
    <key>com.apple.security.app-sandbox</key><true/>
    <key>com.apple.security.cs.allow-jit</key><true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key><true/>
    <key>com.apple.security.cs.disable-executable-page-protection</key><true/>
    <key>com.apple.security.inherit</key><true/>
</dict></plist>
EOF


# Remove quarantine from the entire .app bundle (portable, no -r needed)
while IFS= read -r -d '' f; do
  xattr -d com.apple.quarantine "$f" 2>/dev/null || true
done < <(find "$APP" -print0)

# Sanity check: should print nothing
find "$APP" -print0 | xargs -0 xattr -l 2>/dev/null | grep -i quarantine || echo "No quarantine xattrs found."

# 8) Sign ALL nested Mach-O binaries with proper sandbox entitlements
while IFS= read -r -d '' f; do
  if file -b "$f" | grep -Eq 'Mach-O (64-bit|universal)'; then
    echo "Signing nested: $f"

    # Java runtime executables need sandbox entitlements
    if [[ "$f" == *"/bin/java" ]] || [[ "$f" == *"/jspawnhelper" ]]; then
      codesign --force --timestamp --options runtime \
        --entitlements /tmp/java_sandbox_entitlements.plist \
        -s "$SIGN_ID" "$f"
    else
      codesign --force --timestamp --options runtime \
        --entitlements "$ENT" \
        -s "$SIGN_ID" "$f"
    fi
  fi
done < <(find "$APP/Contents" -type f \( -perm -111 -o -name '*.dylib' -o -name '*.jnilib' \) -print0)

# 9) Sign the top-level .app
codesign --force --timestamp --options runtime \
  --entitlements "$APP_ENT" \
  -s "$SIGN_ID" "$APP"

# Cleanup
rm -f /tmp/java_sandbox_entitlements.plist
rm -f "$APP_ENT"

# 10) Verify signing and plist
codesign --verify --deep --strict --verbose=5 "$APP"
spctl --assess --type execute --verbose=4 "$APP" || true

# Verify the minimum system version was set correctly
echo "Verifying minimum macOS version:"
/usr/libexec/PlistBuddy -c "Print :LSMinimumSystemVersion" "$PLIST" || echo "LSMinimumSystemVersion not found"

# Check that Java binaries are properly sandboxed (Note: with stripped runtime, these may not exist)
echo "Checking for Java runtime binaries:"
if [[ -f "$APP/Contents/runtime/Contents/Home/bin/java" ]]; then
    echo "java executable found - checking entitlements:"
    codesign -d --entitlements :- "$APP/Contents/runtime/Contents/Home/bin/java" | grep -A1 "app-sandbox" || echo "WARNING: java not sandboxed"
else
    echo "No java executable found (expected with stripped runtime)"
fi

if [[ -f "$APP/Contents/runtime/Contents/Home/lib/jspawnhelper" ]]; then
    echo "jspawnhelper found - checking entitlements:"
    codesign -d --entitlements :- "$APP/Contents/runtime/Contents/Home/lib/jspawnhelper" | grep -A1 "app-sandbox" || echo "WARNING: jspawnhelper not sandboxed"
else
    echo "No jspawnhelper found (expected with stripped runtime)"
fi

## check if quarantine gone
#if xattr -lr "$APP" | grep -qi quarantine; then
#  echo "ERROR: quarantine still present"; exit 1
#fi
# 6b) Strip quarantine from everything inside the app (recursive, portable)
while IFS= read -r -d '' f; do
  xattr -d com.apple.quarantine "$f" 2>/dev/null || true
done < <(find "$APP" -print0)

# Verify nothing left:
if find "$APP" -print0 | xargs -0 xattr -l 2>/dev/null | grep -qi quarantine; then
  echo "ERROR: quarantine still present"; exit 1
fi



# 11) Build installer package
productbuild \
  --sign "3rd Party Mac Developer Installer: Jeremy Theodore Zay (9A9HR6WT4K)" \
  --component "$DEST/$APPNAME.app" /Applications \
  "$DEST/$APPNAME-MAS.pkg"

pkgutil --check-signature "$DEST/$APPNAME-MAS.pkg"

echo "✅ Ready for App Store submission: $DEST/$APPNAME-MAS.pkg"
