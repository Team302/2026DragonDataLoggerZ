# Project Structure

## Pi 4 Logger Receiver (Main Project)

Located in the root directory:

```
src/main/java/pi/logger/
├── ReceiverMain.java       # Entry point for Pi logger
├── UdpReceiver.java        # UDP socket listener
├── CsvFileLogger.java      # CSV file writer
└── Logger.java             # Logging interface
```

**Build & Run:**
```bash
./gradlew build
java -jar build/libs/pi-logger.jar --usb 5800
```

**Features:**
- ✅ Listen for UDP telemetry on port 5800
- ✅ Log to CSV with dynamic headers
- ✅ Auto-detect and log to external USB
- ✅ Support flexible field schemas

See [README.md](README.md) for full documentation.

---

## roboRIO Telemetry Sender (Reference)

Located in `src/main/java/pi/logger/sender/` directory:

```
src/main/java/pi/logger/sender/
├── README.md                              # Integration & setup guide
└── RoboRIOTelemetrySender.java           # Sender library for roboRIO projects
```

**What's Included:**
- `RoboRIOTelemetrySender.java` - Core UDP sender class
  - `sendLegacy()` - Send 4-field telemetry (voltage, motorId, speed, current)
  - `sendFrcDriveTrain()` - Send 8-field drivetrain data (+ gyro, encoders)
  - `sendCustom()` - Send arbitrary fields
  - Built-in 20 Hz throttle & ISO-8601 timestamps

- `Robot.java` - Complete example showing:
  - Initialization in `robotInit()`
  - Sending data in teleop/autonomous
  - Proper cleanup in `disabledInit()`

**Usage:**
1. Copy `RoboRIOTelemetrySender.java` to your roboRIO project
2. Update Pi IP address in robot code
3. Call sender methods in your periodic functions
4. Deploy to roboRIO

See [src/main/java/pi/logger/sender/README.md](src/main/java/pi/logger/sender/README.md) for detailed setup.

---

## Architecture

```
┌─────────────────────────┐              ┌──────────────────────────┐
│   roboRIO               │    UDP       │  Raspberry Pi 4          │
│  (Sends telemetry)      │────────────> │  (This logger receiver)  │
│                         │   CSV        │                          │
│ RoboRIOTelemetrySender  │   packets    │ • Receives UDP packets   │
│ • sendFrcDriveTrain()   │   via LAN    │ • Logs to CSV file       │
│ • sendLegacy()          │              │ • Optional USB storage   │
│ • sendCustom()          │              │                          │
└─────────────────────────┘              └──────────────────────────┘
```

## Network Setup

- **roboRIO:** `10.TEAM.2` (e.g., `10.2826.2`)
- **Pi 4:** `10.TEAM.100` (e.g., `10.2826.100`) — Update this in your robot code!
- **UDP Port:** 5800 (configurable)

## Quick Reference

**Start Pi logger:**
```bash
java -jar build/libs/pi-logger.jar --usb 5800
```

**View live logs:**
```bash
tail -f logs/telemetry.csv
```

**roboRIO robot code (minimal example):**
```java
private RoboRIOTelemetrySender telemetrySender;

@Override
public void robotInit() {
    try {
        telemetrySender = new RoboRIOTelemetrySender("10.2826.100", 5800);
    } catch (Exception e) {
        System.err.println("Telemetry init failed: " + e.getMessage());
    }
}

@Override
public void teleopPeriodic() {
    try {
        telemetrySender.sendFrcDriveTrain(
            pdp.getVoltage(), 
            leftMotor.get(), pdp.getCurrent(0),
            rightMotor.get(), pdp.getCurrent(1),
            gyro.getAngle(),
            leftEncoder.getDistance(), rightEncoder.getDistance()
        );
    } catch (Exception e) {
        System.err.println("Telemetry send error: " + e.getMessage());
    }
}

@Override
public void disabledInit() {
    if (telemetrySender != null) telemetrySender.close();
}
```

## CSV Output Format

**Dynamic header** (fields determined by first packet):
```csv
timestamp,voltage,driveLeftSpeed,driveLeftCurrent,driveRightSpeed,driveRightCurrent,gyroAngle,leftEncoderDist,rightEncoderDist
2026-01-24T20:15:30.123456789Z,12.50,0.750,22.5,0.750,21.3,5.2,1.234,1.230
2026-01-24T20:15:30.175234812Z,12.49,0.752,23.1,0.748,21.9,5.4,1.245,1.241
```

## Questions?

- **Pi logger issues?** → See [README.md](README.md)
- **roboRIO sender issues?** → See [roborio-sender/README.md](roborio-sender/README.md)
- **Network issues?** → Check network connectivity: `ping <pi-ip>` from roboRIO SSH
