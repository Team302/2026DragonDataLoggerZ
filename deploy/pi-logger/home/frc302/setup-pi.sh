#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Pi Deploy Starting ==="

bash "$SCRIPT_DIR/scripts/01-packages.sh"
bash "$SCRIPT_DIR/scripts/02-usb-setup.sh"
bash "$SCRIPT_DIR/scripts/03-ntp-setup.sh"
bash "$SCRIPT_DIR/scripts/04-pilogger-service.sh"
bash "$SCRIPT_DIR/scripts/05-update-pilogger.sh"

touch /home/frc302/this_pi_has_been_setup
echo "=== Pi Deploy Complete ==="
