#!/bin/bash
set -e
echo "--- Configuring hardware watchdog ---"

# Enable the BCM2835 hardware watchdog
BOOT_CONFIG="/boot/firmware/config.txt"
if [ ! -f "$BOOT_CONFIG" ]; then
    BOOT_CONFIG="/boot/config.txt"
fi

if grep -Eq '^[[:space:]]*dtparam=watchdog=on([[:space:]]*(#.*)?)?$' "$BOOT_CONFIG"; then
    echo "Watchdog already enabled in $BOOT_CONFIG"
elif grep -Eq '^[[:space:]]*dtparam=watchdog(=.*)?([[:space:]]*(#.*)?)?$' "$BOOT_CONFIG"; then
    sudo sed -i -E 's/^[[:space:]]*dtparam=watchdog(=.*)?([[:space:]]*(#.*)?)?$/dtparam=watchdog=on/' "$BOOT_CONFIG"
    echo "Updated watchdog setting to enabled in $BOOT_CONFIG"
else
    echo 'dtparam=watchdog=on' | sudo tee -a "$BOOT_CONFIG" > /dev/null
    echo "Watchdog enabled in $BOOT_CONFIG"
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