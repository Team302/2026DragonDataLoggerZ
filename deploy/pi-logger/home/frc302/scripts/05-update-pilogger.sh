#!/bin/bash
set -e

JAR_SRC="/tmp/PiLogger-linuxarm64-cross.jar"
JAR_DEST="/home/frc302/PiLogger-linuxarm64-cross.jar"

if [ ! -f "$JAR_SRC" ]; then
    echo "ERROR: $JAR_SRC not found, aborting"
    exit 1
fi

sudo systemctl stop pilogger.service
sudo install -o frc302 -g frc302 -m 644 "$JAR_SRC" "$JAR_DEST"
sudo systemctl start pilogger.service
sudo systemctl status pilogger.service