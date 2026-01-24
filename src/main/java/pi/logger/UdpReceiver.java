package pi.logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Simple UDP receiver that expects CSV payloads and writes LogRecord entries via the Logger.
 * Expected CSV formats (either):
 * 1) voltage,motorId,motorSpeed,motorCurrent
 * 2) timestampISO,voltage,motorId,motorSpeed,motorCurrent
 */
public class UdpReceiver implements AutoCloseable {
    private final DatagramSocket socket;
    private final Thread worker;
    private volatile boolean running = true;
    private final Logger logger;

    public UdpReceiver(Logger logger, int port) throws SocketException {
        this.logger = logger;
        this.socket = new DatagramSocket(port);
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
                if (s.isEmpty()) continue;
                String[] parts = s.split(",");
                Instant ts = Instant.now();

                int start = 0;
                if (parts.length >= 5) {
                    // try parse timestamp
                    try {
                        ts = Instant.parse(parts[0]);
                        start = 1;
                    } catch (Exception ignored) {
                        // not a timestamp, keep now and parse from 0
                        start = 0;
                    }
                }

                // expect remaining: voltage,motorId,motorSpeed,motorCurrent
                if (parts.length - start < 4) continue;
                double voltage = Double.parseDouble(parts[start + 0]);
                String motorId = parts[start + 1];
                double speed = Double.parseDouble(parts[start + 2]);
                double current = Double.parseDouble(parts[start + 3]);

                LogRecord r = new LogRecord(ts, voltage, motorId, speed, current);
                logger.log(r);
            } catch (Exception e) {
                if (running) e.printStackTrace();
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
