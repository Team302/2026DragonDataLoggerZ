package pi.logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UDP receiver that expects CSV payloads and writes LogRecord entries via the Logger.
 * Supports flexible field formats:
 * 1) Fixed 4-field legacy: voltage,motorId,motorSpeed,motorCurrent
 * 2) Fixed 5-field with timestamp: timestampISO,voltage,motorId,motorSpeed,motorCurrent
 * 3) Dynamic fields (if first line is header): field1,field2,field3,...
 *    followed by data: value1,value2,value3,...
 */
public class UdpReceiver implements AutoCloseable {
    /**
     * Immutable telemetry record holder with dynamic fields.
     * Stores timestamp and a map of field names to values.
     */
    public static class LogRecord {
        private final Instant timestamp;
        private final Map<String, String> fields;  // field name -> value (as string)

        public LogRecord(Instant timestamp, Map<String, String> fields) {
            this.timestamp = timestamp;
            this.fields = new LinkedHashMap<>(fields);  // preserve insertion order
        }

        public Instant getTimestamp() { return timestamp; }
        public Map<String, String> getFields() { return new LinkedHashMap<>(fields); }
        
        // Legacy accessors for backward compatibility
        public double getVoltage() { return parseDouble(fields.get("voltage"), 0.0); }
        public String getMotorId() { return fields.getOrDefault("motorId", ""); }
        public double getMotorSpeed() { return parseDouble(fields.get("motorSpeed"), 0.0); }
        public double getMotorCurrent() { return parseDouble(fields.get("motorCurrent"), 0.0); }

        private static double parseDouble(String s, double def) {
            try { return s != null ? Double.parseDouble(s) : def; }
            catch (NumberFormatException e) { return def; }
        }

        @Override
        public String toString() {
            return "LogRecord{timestamp=" + timestamp + ", fields=" + fields + '}';
        }
    }

    private final DatagramSocket socket;
    private final Thread worker;
    private volatile boolean running = true;
    private final Logger logger;

    public UdpReceiver(Logger logger, int port) throws SocketException {
        this.logger = logger;
        this.socket = new DatagramSocket(port);
        this.socket.setSoTimeout(10000);  // 10 second timeout for receive
        this.worker = new Thread(this::loop, "UdpReceiver-Thread");
        worker.setDaemon(true);
        worker.start();
    }

    private void loop() {
        byte[] buf = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (running && !socket.isClosed()) {
            try {
                socket.receive(packet);
                String s = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8).trim();
                System.out.println("[UdpReceiver] Raw: " + s);
                if (s.isEmpty()) continue;
                
                String[] parts = s.split(",");
                System.out.println("[UdpReceiver] Parts count: " + parts.length);
                
                Instant ts = Instant.now();
                Map<String, String> fields = new LinkedHashMap<>();

                // Try to detect and parse timestamp in first field
                int startIndex = 0;
                if (parts.length >= 2) {
                    try {
                        ts = Instant.parse(parts[0].trim());
                        startIndex = 1;
                        System.out.println("[UdpReceiver] Parsed timestamp: " + ts);
                    } catch (Exception ignored) {
                        startIndex = 0;
                    }
                }

                int remaining = parts.length - startIndex;

                // Legacy 4-field format: voltage,motorId,motorSpeed,motorCurrent
                if (remaining == 4) {
                    fields.put("voltage", parts[startIndex].trim());
                    fields.put("motorId", parts[startIndex + 1].trim());
                    fields.put("motorSpeed", parts[startIndex + 2].trim());
                    fields.put("motorCurrent", parts[startIndex + 3].trim());
                } 
                // Generic N-field format (at least 2 fields to make sense)
                else if (remaining >= 2) {
                    // Use sequential field names if custom names not provided
                    for (int i = 0; i < remaining; i++) {
                        String fieldName = "field" + (i + 1);
                        fields.put(fieldName, parts[startIndex + i].trim());
                    }
                } else {
                    System.out.println("[UdpReceiver] Skipping packet: insufficient fields (" + remaining + ")");
                    continue;
                }

                LogRecord r = new LogRecord(ts, fields);
                logger.log(r);
            } catch (Exception e) {
                if (running) {
                    System.err.println("[UdpReceiver] Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (Exception ignored) {
        }
        try {
            worker.join(500);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
