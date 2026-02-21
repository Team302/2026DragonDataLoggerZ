# 2026DragonDataLoggerZ 🍓 π 🚀🔥

## Overview 

This program turns a small computer (like a Raspberry Pi ) into a black box for our FRC robot. It listens to the robot’s NetworkTables traffic, collects drive data sent over UDP, and saves everything as WPILOG files on a USB drive. After a match or practice run we can pull the USB stick, drop the file into AdvantageScope or the WPILOG viewer, and replay what the robot was doing. π

No cap, this logger is a W for your drive team. Let's go! 🚀

### What it watches 👀

- **Drive state:** Pose, chassis speeds, and every swerve module’s position, state, and target. 🏎️
- **Match info:** Current match number plus whether the robot is enabled or disabled. 🏁
- **Health stats:** CPU load, disk space, queue depth, and a heartbeat so we know the logger is alive. 💓
- **Custom telemetry:** Any CSV-formatted packets sent to UDP port 5900 (for example from a roboRIO or coprocessor). 📡

### How it works 🤔

1. **NT client startup:** `NtClient` connects to the team or custom server (default team 302, or you can config an IP/hostname). This keeps the Pi in sync with the robot’s NetworkTables. (π) 🤝
2. **NetworkTables logging:** `NetworkTablesLogger` subscribes to the DriveState table, converts the structs, and emits `TelemetryEvent`s instead of writing directly to disk. 💾
3. **UDP receiver:** `UdpReceiver` listens on port 5900 for CSV packets (`timestamp,signalID,type,value,units`) and emits matching telemetry events as soon as packets arrive. 📥
4. **Telemetry processor:** `TelemetryProcessor` is the new middle layer. It ingests events from both sources, runs any registered `TelemetryStage`s (mix, filter, enrich, metrics), and decides what ultimately gets persisted. Want custom math or feature flags? Drop in another stage. 🧠
5. **USB file writer:** `USBFileLogger` now focuses purely on file lifecycle (rotation, flushing). The default stages call its APIs directly to write `/mnt/usb_logs/*.wpilog`, so adding new sinks or copying data elsewhere is straightforward. 🔄
6. **Health + match publishing:** `HealthPublisher` reports health data back to NetworkTables, while `MatchInfoListener` keeps track of match status. 🩺

### Why it matters💡

- Gives drivers and programmers fast feedback after every run. W move! 🏆
- Helps catch flaky sensors, brownouts, or bad odometry before playoffs. No Ls here! 🛡️
- Creates a long-term archive of drive data we can study between events. Vibe check: passed! 📈

### Getting Started 🛠️

1. Build the project with `./gradlew build` (or `./build.bat` on Windows).
2. Test the project on Windows using run.bat (this will start udp listening locally)
3. If deploying to the pi, run build-pi.bat. This will create a jar in the build/libs folder called PiLogger-linuxarm64-cross.jar
4. Copy the runnable jar, PiLogger-linuxarm64-cross.jar,  (this includes all necessary libs)  to the Pi.
5. Plug in a USB drive mounted at `/mnt/usb_logs` and run the logger.
6. After the match, remove the drive and open the `.wpilog` file in your viewer of choice.

### Continuous Integration 🤖

Every push and pull request to `main` automatically runs a Gradle build via GitHub Actions (see `.github/workflows/gradle-build.yml`). The workflow uses Temurin JDK 21, validates the Gradle wrapper, assembles both the standard and shadow jars, and uploads the contents of `build/libs` and `build/distributions` as artifacts so you can download the packaged binaries straight from the run summary.

### Setup Instructions for π 🤖
1. Flash the Pi with the Pi4 image using Raspberry Pi Imager (EZ mode)
2. Pick Raspberry Pi OS Lite (64 bit) (no bloat, just vibes)
    1. Name the Pi `pi-logger`
    2. Have it connect to Wi‑Fi for the next step
    3. Create user `frc302` with password `dragons`
3. Insert the SD card into the Pi (let it cook)
4. Run the following on the Pi to setup software (chef’s kiss):

    ```bash
    sudo apt-get update
    sudo apt-get upgrade
    sudo apt-get install openjdk-21-jdk
    ```    
5. Run the following on the pi to setup the usb drive (so your logs don’t ghost you):
   
   ```bash
   sudo mkdir -p /mnt/usb_logs
   sudo chown frc302:frc302 /mnt/usb_logs
   
   # Update /etc/fstab with this line
   LABEL=ROBOT_LOGS /mnt/usb_logs vfat defaults,nofail,noatime,uid=1000,gid=1000,umask=0022 0 0
   
   ```
5. Create a usb stick that is formatted FAT32 with the label ROBOT_LOGS
5. Insert usb stick into PI usb slot
6. Follow the [build instructions](docs/boot_script_instructions.md) above for the rest of the setup. You’re built different now. ✨


### Enable / Disable the Wifi on the PI
To disable the wifi, run the following command on the pi:

```bash
sudo nano /boot/firmware/config.txt
```

Then add the following line to the end of the file:

```dtoverlay=disable-wifi
```
To re-enable the wifi, simply remove that line from the config.txt file.    