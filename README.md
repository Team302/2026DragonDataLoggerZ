# 2026DragonDataLoggerZ

## Overview

This program turns a small computer (like a Raspberry Pi) into a black box for our FRC robot. It listens to the robot’s NetworkTables traffic, collects drive data sent over UDP, and saves everything as WPILOG files on a USB drive. After a match or practice run we can pull the USB stick, drop the file into AdvantageScope or the WPILOG viewer, and replay what the robot was doing.

### What it watches

- **Drive state:** Pose, chassis speeds, and every swerve module’s position, state, and target.
- **Match info:** Current match number plus whether the robot is enabled or disabled.
- **Health stats:** CPU load, disk space, queue depth, and a heartbeat so we know the logger is alive.
- **Custom telemetry:** Any CSV-formatted packets sent to UDP port 5900 (for example from a roboRIO or coprocessor).

### How it works

1. **NT client startup:** `NtClient` connects to the team or custom server (default team 302, or you can pass an IP/hostname). This keeps the Pi in sync with the robot’s NetworkTables.
2. **NetworkTables logging:** `NetworkTablesLogger` subscribes to the DriveState table, converts the structs, and streams them to the USB logger.
3. **UDP receiver:** `UdpReceiver` listens on port 5900 for CSV packets (`timestamp,signalID,type,value,units`) and drops them into a queue.
4. **USB file writer:** `USBFileLogger` pulls from the queue, rotates files every five minutes, and writes everything into `/mnt/usb_logs/*.wpilog` so tools can replay it.
5. **Health + match publishing:** `HealthPublisher` reports health data back to NetworkTables, while `MatchInfoListener` keeps track of match status.

### Why it matters

- Gives drivers and programmers fast feedback after every run.
- Helps catch flaky sensors, brownouts, or bad odometry before playoffs.
- Creates a long-term archive of drive data we can study between events.

### Getting Started

1. Build the project with `./gradlew build` (or `./build.bat` on Windows).
2. Test the project on Windows using run.bat (this will start udp listening locally)
3. If deploying to the pi, run build-pi.bat. This will create a jar in the build/libs folder called PiLogger-linuxarm64-cross.jar
2. Copy the runnable jar, PiLogger-linuxarm64-cross.jar,  (this includes all necessary libs)  to the Pi.
3. Plug in a USB drive mounted at `/mnt/usb_logs` and run the logger.
4. After the match, remove the drive and open the `.wpilog` file in your viewer of choice.

### Setup Instructions for PI
1. Flash pi with Pi4 image using Raspberry Pi Imager
2. Picking Raspberry PI OS Lite (64 bit)
    a. Name the pi as pi-logger
    b. Have it connect to wifi for the next step
    c. Create user as frc302 and password as dragons
3. Insert card to pi
4. Run the following:
    a. apt-get update
    b. apt-get upgrade
    c. apt-get install openjdk-21-jdk
5. Make an entry in /etc/fstab for /mnt/usb_logs
6. Following build instructions for the rest of the setup