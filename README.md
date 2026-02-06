# 2026DragonDataLoggerZ 🍓 π 🚀🔥

## Overview 🍓✨

This program turns a small computer (like a Raspberry Pi 🍓) into a black box for our FRC robot 🤖. It listens to the robot’s NetworkTables traffic, collects drive data sent over UDP, and saves everything as WPILOG files 💾 on a USB drive. After a match or practice run we can pull the USB stick, drop the file into AdvantageScope or the WPILOG viewer, and replay what the robot was doing. π

No cap, this logger is a W for your drive team. Let's go! 🚀

### What it watches 🍓👀

- **Drive state:** Pose, chassis speeds, and every swerve module’s position, state, and target. 🏎️
- **Match info:** Current match number plus whether the robot is enabled or disabled. 🏁
- **Health stats:** CPU load, disk space, queue depth, and a heartbeat so we know the logger is alive. 💓
- **Custom telemetry:** Any CSV-formatted packets sent to UDP port 5900 (for example from a roboRIO or coprocessor). 📡

### How it works 🥧 π 🤔

1. **NT client startup:** `NtClient` connects to the team or custom server (default team 302, or you can pass an IP/hostname). This keeps the Pi in sync with the robot’s NetworkTables. (π) 🤝
2. **NetworkTables logging:** `NetworkTablesLogger` subscribes to the DriveState table, converts the structs, and streams them to the USB logger. 💾
3. **UDP receiver:** `UdpReceiver` listens on port 5900 for CSV packets (`timestamp,signalID,type,value,units`) and drops them into a queue. 📥
4. **USB file writer:** `USBFileLogger` pulls from the queue, rotates files every five minutes, and writes everything into `/mnt/usb_logs/*.wpilog` so tools can replay it. 🔄
5. **Health + match publishing:** `HealthPublisher` reports health data back to NetworkTables, while `MatchInfoListener` keeps track of match status. 🩺

### Why it matters 🍓💡

- Gives drivers and programmers fast feedback after every run. W move! 🏆
- Helps catch flaky sensors, brownouts, or bad odometry before playoffs. No Ls here! 🛡️
- Creates a long-term archive of drive data we can study between events. Vibe check: passed! 📈

### Getting Started 🍓🛠️

1. Build the project with `./gradlew build` (or `./build.bat` on Windows).
2. Test the project on Windows using run.bat (this will start udp listening locally)
3. If deploying to the pi, run build-pi.bat. This will create a jar in the build/libs folder called PiLogger-linuxarm64-cross.jar
4. Copy the runnable jar, PiLogger-linuxarm64-cross.jar,  (this includes all necessary libs)  to the Pi.
5. Plug in a USB drive mounted at `/mnt/usb_logs` and run the logger.
6. After the match, remove the drive and open the `.wpilog` file in your viewer of choice.

### Setup Instructions for Pi 🍓 π 🤖
1. Flash the Pi with the Pi4 image using Raspberry Pi Imager 🍓 (EZ mode)
2. Pick Raspberry Pi OS Lite (64 bit) (no bloat, just vibes)
    1. Name the Pi `pi-logger`
    2. Have it connect to Wi‑Fi for the next step
    3. Create user `frc302` with password `dragons`
3. Insert the SD card into the Pi (let it cook)
4. Run the following on the Pi (chef’s kiss):

    ```bash
    apt-get update
    apt-get upgrade
    apt-get install openjdk-21-jdk
    ```
5. Make an entry in `/etc/fstab` for `/mnt/usb_logs` (so your logs don’t ghost you)
6. Follow the build instructions above for the rest of the setup. You’re built different now. ✨