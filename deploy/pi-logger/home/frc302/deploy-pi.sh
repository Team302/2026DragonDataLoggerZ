#!/bin/bash
sudo systemctl stop pilogger.service
sudo cp /tmp/PiLogger-linuxarm64-cross.jar /home/frc302/PiLogger-linuxarm64-cross.jar
sudo systemctl start pilogger.service
sudo systemctl status pilogger.service