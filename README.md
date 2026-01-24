# 2026 Dragon Data Logger Z

**UDP Telemetry Receiver for Raspberry Pi 4**

Receives robot telemetry from roboRIO via UDP and logs it to CSV files (local or USB storage).

## Architecture

```
┌─────────────────────┐              ┌──────────────────────┐
│   roboRIO           │    UDP       │  Raspberry Pi 4      │
│  (Sends telemetry)  │────────────> │  (This logger)       │
│                     │   CSV        │  • Receives UDP      │
│ • Motor speeds      │   packets    │  • Logs to CSV       │
│ • Motor currents    │   over LAN   │  • Local or USB      │
│ • Voltage, etc.     │              │                      │
└─────────────────────┘              └──────────────────────┘
```

## Quick Start

### On Raspberry Pi 4

```bash
# 1. Install JDK 17
sudo apt update && sudo apt install -y openjdk-17-jre-headless

# 2. Build or copy the jar
gradle build

# 3. Run receiver (listens on UDP port 5800)
java -jar build/libs/pi-logger.jar

# Or with USB auto-detection
java -jar build/libs/pi-logger.jar --usb 5800

# 4. View logs
tail -f logs/telemetry.csv
```

## Command Line Usage

```bash
# Default: listen on port 5800, log to logs/telemetry.csv
java -jar build/libs/pi-logger.jar

# Custom port and path
java -jar build/libs/pi-logger.jar /path/to/logs.csv 5801

# Auto-detect USB storage (50MB min free space)
java -jar build/libs/pi-logger.jar --usb 5800

# USB with specific label
java -jar build/libs/pi-logger.jar --usb-label "STORAGE" 5800

# Environment variables
LOG_PATH=/mnt/usb/telemetry.csv UDP_PORT=5800 java -jar build/libs/pi-logger.jar
```

## CSV Log Format

Dynamic format based on first packet received:

```csv
timestamp,voltage,motorId,motorSpeed,motorCurrent
2026-01-24T20:15:30.123456789Z,12.50,driveLeft,0.750,22.5
2026-01-24T20:15:30.175234812Z,12.50,driveRight,0.750,21.3
```

## roboRIO Sender Setup

The roboRIO sends telemetry via UDP to the Pi. Use the `RoboRIOTelemetrySender` class (included in `build/libs/pi-logger.jar`):

```java
import pi.logger.RoboRIOTelemetrySender;

// In robot code initialization
RoboRIOTelemetrySender sender = new RoboRIOTelemetrySender("10.XX.XX.100", 5800);

// In your periodic loop
sender.sendLegacy(
    pdp.getVoltage(),      // battery voltage
    "driveLeft",           // motor ID
    leftMotor.get(),       // motor output
    pdp.getCurrent(0)      // motor current
);

// Or send custom fields
Map<String, String> fields = new LinkedHashMap<>();
fields.put("voltage", String.valueOf(pdp.getVoltage()));
fields.put("gyroAngle", String.valueOf(gyro.getAngle()));
sender.sendCustom(fields);
```

## Architecture & Files

**Pi Logger (Receiver)**
- `src/main/java/pi/logger/ReceiverMain.java` - Receiver entrypoint; handles CLI args, USB detection
- `src/main/java/pi/logger/UdpReceiver.java` - UDP socket listener; parses CSV packets
- `src/main/java/pi/logger/CsvFileLogger.java` - CSV file writer; dynamic headers
- `src/main/java/pi/logger/Logger.java` - Logging interface

**roboRIO Sender (Reference)**
- `src/main/java/pi/logger/sender/RoboRIOTelemetrySender.java` - UDP sender class for roboRIO projects
  - Copy this to your roboRIO code: `src/main/java/frc/robot/telemetry/RoboRIOTelemetrySender.java`
  - See `src/main/java/pi/logger/sender/README.md` for integration details

## Build

```bash
gradle build
# Outputs: build/libs/pi-logger.jar (~500 KB)
```

## Troubleshooting

**Receiver not receiving packets:**
- Check network: `ping <pi-ip-address>` from roboRIO
- Verify port: `netstat -tuln | grep 5800` on Pi
- Check firewall rules on Pi

**USB not detected:**
- Verify USB is mounted: `lsblk` on Pi
- Check free space: `df -h | grep /mnt`
- Try with explicit path: `java -jar build/libs/pi-logger.jar /mnt/usb/logs.csv 5800`

**Low performance / packet loss:**
- Reduce send frequency from roboRIO (default ~20 Hz)
- Check network congestion and latency
- Use wired connection instead of WiFi if possible