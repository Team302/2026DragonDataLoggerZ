#!/bin/bash
set -e

TEAM_IP="${1:-10.3.2.2}"  # pass as arg or set default here
CONF_FILE="/etc/systemd/timesyncd.conf"

echo "--- Configuring NTP to $TEAM_IP ---"
sudo tee "$CONF_FILE" > /dev/null <<EOF
[Time]
NTP=$TEAM_IP
FallbackNTP=pool.ntp.org
EOF

sudo systemctl restart systemd-timesyncd
timedatectl status
echo "--- NTP setup done ---"
