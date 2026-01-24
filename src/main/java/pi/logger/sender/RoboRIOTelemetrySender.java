package pi.logger.sender;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends robot telemetry data via UDP to a remote logging server (e.g., Raspberry Pi).
 * 
 * Example usage in roboRIO Robot.java:
 * <pre>
 * import pi.logger.sender.RoboRIOTelemetrySender;
 * 
 * private RoboRIOTelemetrySender telemetrySender;
 * 
 * @Override
 * public void robotInit() {
 *     try {
 *         telemetrySender = new RoboRIOTelemetrySender("10.2826.100", 5800);
 *     } catch (Exception e) {
 *         System.err.println("Failed to init telemetry: " + e.getMessage());
 *     }
 * }
 * 
 * @Override
 * public void teleopPeriodic() {
 *     try {
 *         telemetrySender.sendFrcDriveTrain(
 *             pdp.getVoltage(),
 *             leftMotor.get(), pdp.getCurrent(0),
 *             rightMotor.get(), pdp.getCurrent(1),
 *             gyro.getAngle(),
 *             leftEncoder.getDistance(), rightEncoder.getDistance()
 *         );
 *     } catch (Exception e) {
 *         System.err.println("Telemetry error: " + e.getMessage());
 *     }
 * }
 * 
 * @Override
 * public void disabledInit() {
 *     if (telemetrySender != null) {
 *         telemetrySender.close();
 *     }
 * }
 * </pre>
 */
public class RoboRIOTelemetrySender implements AutoCloseable {
    private DatagramSocket socket;
    private InetAddress piAddress;
    private int piPort;
    private long lastSendTime = 0;
    private static final long SEND_INTERVAL_MS = 50; // ~20 Hz throttle

    /**
     * Creates a new telemetry sender.
     * 
     * @param piIpAddress IP address of the Pi logger (e.g., "10.2826.100")
     * @param piPort UDP port on the Pi logger (e.g., 5800)
     * @throws Exception if socket creation fails
     */
    public RoboRIOTelemetrySender(String piIpAddress, int piPort) throws Exception {
        this.socket = new DatagramSocket();
        this.piAddress = InetAddress.getByName(piIpAddress);
        this.piPort = piPort;
    }

    /**
     * Sends legacy 4-field telemetry (voltage, motorId, motorSpeed, motorCurrent).
     * Includes ISO-8601 timestamp from roboRIO.
     * 
     * @param voltage Battery voltage (e.g., 12.5V)
     * @param motorId Motor identifier (e.g., "driveLeft", "driveRight", "intake")
     * @param motorSpeed Motor output velocity (-1.0 to 1.0)
     * @param motorCurrent Motor current draw (amps)
     * @throws Exception if send fails
     */
    public void sendLegacy(double voltage, String motorId, double motorSpeed, double motorCurrent) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("voltage", String.format("%.2f", voltage));
        fields.put("motorId", motorId);
        fields.put("motorSpeed", String.format("%.3f", motorSpeed));
        fields.put("motorCurrent", String.format("%.1f", motorCurrent));
        sendCustom(fields);
    }

    /**
     * Sends extended FRC drivetrain telemetry (8 fields).
     * Useful for autonomous tuning and drive characterization.
     * 
     * @param voltage Battery voltage (e.g., 12.5V)
     * @param driveLeftSpeed Left drive speed (-1.0 to 1.0)
     * @param driveLeftCurrent Left drive current (amps)
     * @param driveRightSpeed Right drive speed (-1.0 to 1.0)
     * @param driveRightCurrent Right drive current (amps)
     * @param gyroAngle Gyro heading angle (degrees, 0-360)
     * @param leftEncoderDist Left encoder distance traveled (meters)
     * @param rightEncoderDist Right encoder distance traveled (meters)
     * @throws Exception if send fails
     */
    public void sendFrcDriveTrain(double voltage, double driveLeftSpeed, double driveLeftCurrent,
            double driveRightSpeed, double driveRightCurrent, double gyroAngle,
            double leftEncoderDist, double rightEncoderDist) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("voltage", String.format("%.2f", voltage));
        fields.put("driveLeftSpeed", String.format("%.3f", driveLeftSpeed));
        fields.put("driveLeftCurrent", String.format("%.1f", driveLeftCurrent));
        fields.put("driveRightSpeed", String.format("%.3f", driveRightSpeed));
        fields.put("driveRightCurrent", String.format("%.1f", driveRightCurrent));
        fields.put("gyroAngle", String.format("%.1f", gyroAngle));
        fields.put("leftEncoderDist", String.format("%.3f", leftEncoderDist));
        fields.put("rightEncoderDist", String.format("%.3f", rightEncoderDist));
        sendCustom(fields);
    }

    /**
     * Sends arbitrary custom fields to the logger.
     * Field order is preserved (use LinkedHashMap in caller if order matters).
     * Timestamp is added automatically.
     * 
     * Example:
     * <pre>
     * Map<String, String> fields = new LinkedHashMap<>();
     * fields.put("voltage", "12.50");
     * fields.put("intakeCurrent", "5.2");
     * fields.put("shooterSpeed", "3500");
     * sender.sendCustom(fields);
     * </pre>
     * 
     * @param fields Map of field names to string values
     * @throws Exception if send fails
     */
    public void sendCustom(Map<String, String> fields) throws Exception {
        throttle();
        
        // Add ISO-8601 timestamp
        String timestamp = Instant.now().toString();
        StringBuilder payload = new StringBuilder(timestamp);
        
        // Append all field values
        for (String value : fields.values()) {
            payload.append(",").append(value);
        }
        
        // Send UDP packet
        byte[] data = payload.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, piAddress, piPort);
        socket.send(packet);
    }

    /**
     * Throttles sends to ~20 Hz (50ms between packets).
     * Prevents network congestion and excessive logging on the Pi.
     */
    private void throttle() {
        long now = System.currentTimeMillis();
        if (lastSendTime > 0) {
            long elapsed = now - lastSendTime;
            if (elapsed < SEND_INTERVAL_MS) {
                try {
                    Thread.sleep(SEND_INTERVAL_MS - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        lastSendTime = System.currentTimeMillis();
    }

    /**
     * Closes the UDP socket and releases resources.
     */
    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
