#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Pi Deploy Starting ==="

bash "$SCRIPT_DIR/scripts/05-update-pilogger.sh"
echo "=== Pi Deploy Complete ==="