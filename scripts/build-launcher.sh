#!/usr/bin/env bash
# Build Firebolt Home (launcher/) on your own computer. Needs JDK 17 and the Android SDK (ANDROID_HOME set,
# or Android Studio installed). Without Android tooling, use the launcher-apk workflow on GitHub instead.
# With KEYSTORE_FILE (+ KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD) set, signs with your release key, the same
# one CI uses, so local and CI builds update each other. Otherwise signs with this computer's debug key:
# fine for a try-out, but such a build and a CI build can't replace each other without an uninstall.
# Usage: scripts/build-launcher.sh   then   TV_HOST=<ip> LAUNCHER=firebolt scripts/launcher.sh setup <apk>
set -euo pipefail
cd "$(dirname "$0")/../launcher"
if [ -n "${KEYSTORE_FILE:-}" ]; then
  ./gradlew assembleRelease --no-daemon
else
  echo "KEYSTORE_FILE not set: signing with the local debug key." >&2
  ./gradlew assembleRelease -PlocalSign --no-daemon
fi
APK="$PWD/app/build/outputs/apk/release/app-release.apk"
echo "Built: $APK"
