#!/bin/bash
set -e
echo "--- Setting up USB log mount ---"
sudo mkdir -p /mnt/usb_logs
sudo chown frc302:frc302 /mnt/usb_logs

# Only add fstab entry if not already present
if ! grep -q "ROBOT_LOGS" /etc/fstab; then
    echo 'LABEL=ROBOT_LOGS /mnt/usb_logs vfat defaults,nofail,noatime,uid=1000,gid=1000,umask=0022 0 0' | sudo tee -a /etc/fstab
else
    echo "fstab entry already exists, skipping"
fi
echo "--- USB setup done ---"
