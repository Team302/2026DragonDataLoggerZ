#!/bin/bash
set -e

TEAM_NUMBER="${1:-302}"
FIRST_OCTET=$((TEAM_NUMBER / 100))
SECOND_OCTET=$((TEAM_NUMBER % 100))
STATIC_IP="10.${FIRST_OCTET}.${SECOND_OCTET}.10"  # e.g. 10.3.2.10
GATEWAY="10.${FIRST_OCTET}.${SECOND_OCTET}.1"

echo "--- Configuring static IP: $STATIC_IP ---"

# Verify nmcli is available
if ! command -v nmcli &>/dev/null; then
    echo "ERROR: nmcli not found — is NetworkManager installed?"
    exit 1
fi

# Find the active wired connection
CON_NAME=$(nmcli -t -f NAME,DEVICE connection show --active 2>/dev/null | grep ':eth\|:en' | cut -d: -f1 | head -n1)

# If no active wired connection, try to find any wired device and create a profile
if [ -z "$CON_NAME" ]; then
    WIRED_DEVICE=$(nmcli -t -f DEVICE,TYPE device 2>/dev/null | grep ':ethernet' | cut -d: -f1 | head -n1)
    
    if [ -n "$WIRED_DEVICE" ]; then
        # Create a dedicated connection profile
        CON_NAME="frc${TEAM_NUMBER}-static"
        nmcli connection add type ethernet ifname "$WIRED_DEVICE" con-name "$CON_NAME" 2>/dev/null || true
    else
        echo "ERROR: No wired Ethernet device found"
        exit 1
    fi
fi

nmcli connection modify "$CON_NAME" \
    ipv4.method manual \
    ipv4.addresses "$STATIC_IP/24" \
    ipv4.gateway "$GATEWAY" \
    ipv4.dns "$GATEWAY" \
    connection.autoconnect yes

nmcli connection up "$CON_NAME" || true

echo "--- Static IP setup done (reboot recommended) ---"