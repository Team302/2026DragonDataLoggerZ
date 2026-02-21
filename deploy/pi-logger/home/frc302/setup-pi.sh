#!/bin/bash
sudo apt-get update 
sudo apt-get upgrade -y
sudo apt-get install -y openjdk-21-jdk tcpdump
sudo mkdir -p /mnt/usb_logs 
sudo chown frc302:frc302 /mnt/usb_logs 
echo 'LABEL=ROBOT_LOGS /mnt/usb_logs vfat defaults,nofail,noatime,uid=1000,gid=1000,umask=0022 0 0' | sudo tee -a /etc/fstab 
sudo systemctl daemon-reload 
sudo systemctl enable pilogger.service 
touch /home/frc302/this_pi_has_been_setup
echo "Pi setup complete."