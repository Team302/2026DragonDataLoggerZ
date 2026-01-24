package pi.logger;

import java.time.Instant;

/**
 * Small immutable holder for a single telemetry line.
 */
public class LogRecord {
    private final Instant timestamp;
    private final double voltage;
    private final String motorId;
    private final double motorSpeed;
    private final double motorCurrent;

    public LogRecord(Instant timestamp, double voltage, String motorId, double motorSpeed, double motorCurrent) {
        this.timestamp = timestamp;
        this.voltage = voltage;
        this.motorId = motorId;
        this.motorSpeed = motorSpeed;
        this.motorCurrent = motorCurrent;
    }

    public Instant getTimestamp() { return timestamp; }
    public double getVoltage() { return voltage; }
    public String getMotorId() { return motorId; }
    public double getMotorSpeed() { return motorSpeed; }
    public double getMotorCurrent() { return motorCurrent; }

    @Override
    public String toString() {
        return "LogRecord{" +
                "timestamp=" + timestamp +
                ", voltage=" + voltage +
                ", motorId='" + motorId + '\'' +
                ", motorSpeed=" + motorSpeed +
                ", motorCurrent=" + motorCurrent +
                '}';
    }
}
