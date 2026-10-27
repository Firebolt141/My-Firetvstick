#!/usr/bin/env bash
# Ad-blocking DNS through Android's Private DNS (Fire OS 8 = Android 11). Blocks the sponsored row and
# much of the tracking the debloat can't reach; it does NOT block YouTube ads.
# Skip this if your router already filters DNS (Pi-hole, AdGuard Home, NextDNS): Private DNS bypasses it.
# Usage: TV_HOST=192.168.1.50 scripts/dns.sh on|off|status
set -euo pipefail
source "$(dirname "$0")/common.sh"
RESOLVER="${RESOLVER:-dns.adguard-dns.com}"
case "${1:-status}" in
  on)
    tv "settings put global private_dns_specifier $RESOLVER"
    tv 'settings put global private_dns_mode hostname'
    echo "Private DNS set to $RESOLVER. Open Prime Video / an app and check it still plays." ;;
  off)
    tv 'settings put global private_dns_mode opportunistic'
    tv 'settings delete global private_dns_specifier'
    echo "Private DNS back to automatic (the stick's default)." ;;
  status) ;;
  *) echo "usage: $0 on|off|status" >&2; exit 2 ;;
esac
echo "mode=$(tv 'settings get global private_dns_mode' | tr -d '\r') specifier=$(tv 'settings get global private_dns_specifier' | tr -d '\r')"
