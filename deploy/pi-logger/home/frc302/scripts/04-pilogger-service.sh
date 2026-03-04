#!/bin/bash
set -e
echo "--- Setting up pilogger service ---"

sudo tee /etc/systemd/system/pilogger.service > /dev/null <<EOF
[Unit]
Description=PiLogger
After=network-online.target
Wants=network-online.target

[Service]
User=frc302
WorkingDirectory=/home/frc302/
ExecStart=/usr/bin/java -jar /home/frc302/PiLogger-linuxarm64-cross.jar
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal
SyslogIdentifier=pilogger

[Install]
WantedBy=multi-user.target
EOF

sudo chown root:root /etc/systemd/system/pilogger.service
sudo chmod 644 /etc/systemd/system/pilogger.service
sudo systemctl enable NetworkManager-wait-online.service
sudo systemctl daemon-reload
sudo systemctl enable pilogger.service
echo "--- pilogger service setup done ---"
