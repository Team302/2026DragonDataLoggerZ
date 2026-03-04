#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Pi Deploy Starting ==="

bash "$SCRIPT_DIR/scripts/01-packages.sh"
echo "=== Pi Deploy Complete ==="