package pi.logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Simple UDP sender for telemetry. Can send either timestamped or non-timestamped CSV lines.
 * Usage: java pi.logger.UdpSender <host> <port> [count] [intervalMs] [timestamped]
 */
public class UdpSender {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: UdpSender <host> <port> [count] [intervalMs] [timestamped]");
            System.exit(2);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int count = args.length >= 3 ? Integer.parseInt(args[2]) : 10;
        int interval = args.length >= 4 ? Integer.parseInt(args[3]) : 500;
        boolean ts = args.length >= 5 && Boolean.parseBoolean(args[4]);

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress addr = InetAddress.getByName(host);
            for (int i = 0; i < count; i++) {
                // Generate a sample payload: voltage,motorId,motorSpeed,motorCurrent
                String motor = (i % 2 == 0) ? "driveLeft" : "driveRight";
                double voltage = 12.0 + ((i % 5) - 2) * 0.1;
                double speed = (i % 10 - 5) * 0.1;
                double current = 5.0 + (i % 20) * 0.5;
                String payload;
                if (ts) {
                    payload = String.format("%s,%.3f,%s,%.3f,%.3f", Instant.now().toString(), voltage, motor, speed, current);
                } else {
                    payload = String.format("%.3f,%s,%.3f,%.3f", voltage, motor, speed, current);
                }

                byte[] buf = payload.getBytes(StandardCharsets.UTF_8);
                DatagramPacket p = new DatagramPacket(buf, buf.length, addr, port);
                socket.send(p);
                System.out.println("Sent: " + payload);
                Thread.sleep(interval);
            }
        }
    }
}
