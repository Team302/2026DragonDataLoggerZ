# roboRIO Telemetry Sender

Reference implementation for sending telemetry from roboRIO to Pi 4 logger.

## Files

- **RoboRIOTelemetrySender.java** - UDP sender class (can be used in roboRIO projects)
  - `sendLegacy(voltage, motorId, motorSpeed, motorCurrent)` - Send 4-field data
  - `sendFrcDriveTrain(...)` - Send 8-field drivetrain + gyro + encoders
  - `sendCustom(Map<String, String>)` - Send arbitrary fields

- **RobotExample.java** - Example usage in roboRIO Robot.java

## Quick Integration

### Step 1: Copy Sender Class to Your roboRIO Project

```bash
# Copy to your roboRIO robot project
cp src/main/java/pi/logger/sender/RoboRIOTelemetrySender.java \
   /path/to/your-robot/src/main/java/frc/robot/telemetry/
```

### Step 2: Update Your Robot.java

```java
import frc.robot.telemetry.RoboRIOTelemetrySender;

public class Robot extends TimedRobot {
    private static final String PI_IP_ADDRESS = "10.2826.100";  // UPDATE THIS!
    private static final int PI_UDP_PORT = 5800;
    
    private RoboRIOTelemetrySender telemetrySender;

    @Override
    public void robotInit() {
        try {
            telemetrySender = new RoboRIOTelemetrySender(PI_IP_ADDRESS, PI_UDP_PORT);
            System.out.println("[Robot] Telemetry initialized");
        } catch (Exception e) {
            System.err.println("[Robot] Telemetry init failed: " + e.getMessage());
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
            System.err.println("[Robot] Telemetry error: " + e.getMessage());
        }
    }

    @Override
    public void disabledInit() {
        if (telemetrySender != null) {
            telemetrySender.close();
        }
    }
}
```

### Step 3: Update Pi IP Address

Replace `"10.2826.100"` with your actual Pi IP address:

```bash
# On the Pi, find IP address
hostname -I
# Output: 192.168.1.100
```

## Network Setup

### FRC Team Network
- roboRIO: `10.TEAM.2` (e.g., `10.2826.2`)
- Pi 4: `10.TEAM.100` (e.g., `10.2826.100`)

### Practice/Test Network
- roboRIO: `192.168.1.20` (typical)
- Pi 4: `192.168.1.100` (typical)

### Verify Connectivity

From roboRIO SSH:
```bash
ping 10.2826.100
# Should see responses
```

## Features

- ✅ Auto-includes ISO-8601 timestamp on roboRIO
- ✅ Built-in ~20 Hz throttle (prevents network congestion)
- ✅ 3 convenience methods: legacy 4-field, extended 8-field, custom fields
- ✅ Clean AutoCloseable pattern for resource cleanup

## Telemetry Format

All data sent as CSV with timestamp:

```
<ISO-8601 timestamp>,<field1>,<field2>,...
2026-01-24T20:15:30.123456789Z,12.50,0.750,22.5,0.750,21.3,5.2,1.234,1.230
```

## View Results

On the Pi, after running a match:

```bash
tail -100 logs/telemetry.csv
```

Example output:
```csv
timestamp,voltage,driveLeftSpeed,driveLeftCurrent,driveRightSpeed,driveRightCurrent,gyroAngle,leftEncoderDist,rightEncoderDist
2026-01-24T20:15:30.123456789Z,12.50,0.750,22.5,0.750,21.3,5.2,1.234,1.230
2026-01-24T20:15:30.175234812Z,12.49,0.752,23.1,0.748,21.9,5.4,1.245,1.241
```

## Troubleshooting

**Connection refused:**
- Verify Pi IP address in robot code
- Check Pi logger is running: `java -jar build/libs/pi-logger.jar --usb 5800`
- Test network: `ping <pi-ip>` from roboRIO SSH

**No data appearing in CSV:**
- Check network connectivity (same LAN)
- Verify firewall allows UDP port 5800
- Check roboRIO can reach Pi: `netstat -an | grep 5800` on roboRIO

**Intermittent packets:**
- Network congestion (use wired connection if possible)
- Reduce send frequency if needed (modify `SEND_INTERVAL_MS`)
- Check for other network traffic on same port

## Configuration

Edit `RoboRIOTelemetrySender.java` to customize:

```java
private static final long SEND_INTERVAL_MS = 50;  // Change throttle rate (ms)
```

- `50` ms = 20 Hz (default, safe for most networks)
- `100` ms = 10 Hz (lower bandwidth)
- `20` ms = 50 Hz (higher bandwidth, more packet loss risk)

## Questions?

See main README.md or roborio-sender directory for more examples.
