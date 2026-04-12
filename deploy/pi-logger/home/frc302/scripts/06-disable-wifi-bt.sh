#!/bin/bash
set -e
echo "--- Disabling WiFi and Bluetooth ---"

# Disable in boot config
BOOT_CONFIG="/boot/firmware/config.txt"
if [ ! -f "$BOOT_CONFIG" ]; then
    BOOT_CONFIG="/boot/config.txt"
fi

if ! grep -q "disable-wifi" "$BOOT_CONFIG"; then
    echo 'dtoverlay=disable-wifi' | sudo tee -a "$BOOT_CONFIG"
else
    echo "WiFi already disabled in $BOOT_CONFIG"
fi

if ! grep -q "disable-bt" "$BOOT_CONFIG"; then
    echo 'dtoverlay=disable-bt' | sudo tee -a "$BOOT_CONFIG"
else
    echo "Bluetooth already disabled in $BOOT_CONFIG"
fi

# Disable bluetooth service
sudo systemctl disable bluetooth 2>/dev/null || true

echo "--- WiFi and Bluetooth disabled (reboot required) ---"