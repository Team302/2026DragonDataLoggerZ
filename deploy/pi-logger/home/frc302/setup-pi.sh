#!/bin/bash
# package install 
sudo apt-get update 
sudo apt-get upgrade -y
sudo apt-get install -y openjdk-21-jdk tcpdump

# usb stick setup
sudo mkdir -p /mnt/usb_logs 
sudo chown frc302:frc302 /mnt/usb_logs 
echo 'LABEL=ROBOT_LOGS /mnt/usb_logs vfat defaults,nofail,noatime,uid=1000,gid=1000,umask=0022 0 0' | sudo tee -a /etc/fstab 

# pilogger service setup
sudo cp /tmp/pilogger.service /etc/systemd/system/pilogger.service
sudo chown root:root /etc/systemd/system/pilogger.service
sudo chmod 644 /etc/systemd/system/pilogger.service
sudo systemctl daemon-reload 
sudo systemctl enable pilogger.service 

touch /home/frc302/this_pi_has_been_setup
echo "Pi setup complete."