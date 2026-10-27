#!/usr/bin/env bash
# One-time: create the signing key for Firebolt Home and print the four GitHub secrets the launcher-apk
# workflow needs. Run on YOUR computer (needs keytool, from any JDK 17+). Keep the .jks file and passwords
# somewhere safe: Android only installs an update signed with the same key, and a lost key means
# uninstalling the launcher (and its settings) to install a new build.
# Usage: scripts/launcher-keystore.sh [path/to/firebolt-home.jks]
set -euo pipefail
KS="${1:-$HOME/.android/firebolt-home.jks}"
ALIAS=firebolt-home
[ -e "$KS" ] && { echo "$KS already exists; not overwriting it. Pass another path, or reuse it." >&2; exit 1; }
command -v keytool >/dev/null || { echo "keytool not found: install a JDK 17+ (e.g. Temurin)." >&2; exit 1; }
mkdir -p "$(dirname "$KS")"
read -r -s -p "Choose a keystore password (6+ chars): " PW; echo
[ ${#PW} -ge 6 ] || { echo "too short" >&2; exit 1; }
keytool -genkeypair -keystore "$KS" -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PW" -keypass "$PW" -dname "CN=Firebolt Home" >/dev/null
chmod 600 "$KS"
echo "Created $KS. Add these in GitHub: repo Settings > Secrets and variables > Actions > New repository secret"
echo "  KEYSTORE_PASSWORD = (the password you typed)"
echo "  KEY_PASSWORD      = (the same password)"
echo "  KEY_ALIAS         = $ALIAS"
echo "  KEYSTORE_BASE64   = the output of:  base64 < \"$KS\" | tr -d '\\n'"
