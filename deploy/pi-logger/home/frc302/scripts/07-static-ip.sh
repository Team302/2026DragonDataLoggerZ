#!/bin/bash
set -e

TEAM_NUMBER="${1:-302}"
STATIC_IP="10.${TEAM_NUMBER:0:1}.${TEAM_NUMBER:1}.10"  # e.g. 10.3.2.10
GATEWAY="10.${TEAM_NUMBER:0:1}.${TEAM_NUMBER:1}.1"
CON_NAME="Wired connection 1"

echo "--- Configuring static IP: $STATIC_IP ---"

# Verify nmcli is available
if ! command -v nmcli &>/dev/null; then
    echo "ERROR: nmcli not found — is NetworkManager installed?"
    exit 1
fi

nmcli connection modify "$CON_NAME" \
    ipv4.method manual \
    ipv4.addresses "$STATIC_IP/24" \
    ipv4.gateway "$GATEWAY" \
    ipv4.dns "$GATEWAY" \
    connection.autoconnect yes

nmcli connection up "$CON_NAME" || true

echo "--- Static IP setup done (reboot recommended) ---"