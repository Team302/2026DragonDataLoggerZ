#!/bin/bash
set -e
echo "--- Installing packages ---"
sudo apt-get update
sudo apt-get upgrade -y
sudo apt-get install -y openjdk-21-jdk tcpdump ffmpeg
echo "--- Packages done ---"
