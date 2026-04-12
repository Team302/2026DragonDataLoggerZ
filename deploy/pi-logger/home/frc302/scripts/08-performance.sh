#!/bin/bash
set -e
echo "--- Configuring performance settings ---"

# Set CPU governor to performance
if [ -f /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor ]; then
    echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
    # Persist across reboots via rc.local
    if [ ! -f /etc/rc.local ]; then
        printf '#!/bin/sh -e\nexit 0\n' | sudo tee /etc/rc.local > /dev/null
        sudo chmod +x /etc/rc.local
    fi
    if ! grep -q "scaling_governor" /etc/rc.local; then
        sudo sed -i '/^exit 0/i echo performance | tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor' /etc/rc.local
    fi
    echo "CPU governor set to performance"
else
    echo "cpufreq not available, skipping governor"
fi

# Reduce SD card wear — log to RAM, not disk
# journal size cap so we don't fill tmpfs
JOURNAL_CONF="/etc/systemd/journald.conf.d/frc302.conf"
sudo mkdir -p /etc/systemd/journald.conf.d
sudo tee "$JOURNAL_CONF" > /dev/null <<EOF
[Journal]
Storage=volatile
RuntimeMaxUse=64M
EOF
sudo systemctl restart systemd-journald
echo "Journal set to volatile (RAM), max 64M"

# Disable swap to reduce SD wear
sudo systemctl disable dphys-swapfile 2>/dev/null || true
sudo swapoff -a 2>/dev/null || true
echo "Swap disabled"

# Mount /tmp as tmpfs if not already
if ! grep -q "tmpfs /tmp" /etc/fstab; then
    echo 'tmpfs /tmp tmpfs defaults,noatime,nosuid,size=64m 0 0' | sudo tee -a /etc/fstab
    echo "tmpfs /tmp added to fstab"
else
    echo "tmpfs /tmp already in fstab"
fi

echo "--- Performance setup done (reboot recommended) ---"