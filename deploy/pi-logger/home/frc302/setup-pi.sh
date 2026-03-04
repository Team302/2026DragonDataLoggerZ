#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Pi Setup Starting ==="

bash "$SCRIPT_DIR/scripts/01-packages.sh"
bash "$SCRIPT_DIR/scripts/02-usb-setup.sh"
bash "$SCRIPT_DIR/scripts/03-ntp-setup.sh"
bash "$SCRIPT_DIR/scripts/04-pilogger-service.sh"
bash "$SCRIPT_DIR/scripts/05-update-pilogger.sh"
bash "$SCRIPT_DIR/scripts/06-disable-wifi-bt.sh"
bash "$SCRIPT_DIR/scripts/07-static-ip.sh"
bash "$SCRIPT_DIR/scripts/08-performance.sh"
bash "$SCRIPT_DIR/scripts/09-watchdog.sh"

touch /home/frc302/this_pi_has_been_setup
echo "=== Pi Setup Complete — reboot recommended ==="