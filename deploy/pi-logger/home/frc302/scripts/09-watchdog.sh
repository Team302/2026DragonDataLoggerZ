#!/bin/bash
set -e
echo "--- Configuring hardware watchdog ---"

# Enable the BCM2835 hardware watchdog
BOOT_CONFIG="/boot/firmware/config.txt"
if [ ! -f "$BOOT_CONFIG" ]; then
    BOOT_CONFIG="/boot/config.txt"
fi

if ! grep -q "dtparam=watchdog" "$BOOT_CONFIG"; then
    echo 'dtparam=watchdog=on' | sudo tee -a "$BOOT_CONFIG"
    echo "Watchdog enabled in $BOOT_CONFIG"
else
    echo "Watchdog already enabled in $BOOT_CONFIG"
fi

# Configure systemd to use the watchdog
WATCHDOG_CONF="/etc/systemd/system.conf.d/watchdog.conf"
sudo mkdir -p /etc/systemd/system.conf.d
sudo tee "$WATCHDOG_CONF" > /dev/null <<EOF
[Manager]
RuntimeWatchdogSec=15
RebootWatchdogSec=2min
EOF
sudo systemctl daemon-reload
echo "--- Watchdog setup done (reboot required) ---"