package pi.logger;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Standalone receiver main. Starts a CsvFileLogger, optional NetworkTableMonitor and the UdpReceiver.
 * Usage: java pi.logger.ReceiverMain [logPath] [udpPort] [enableNt] [ntTable] [ntMotors] [ntIntervalMs]
 */
public class ReceiverMain {
    public static void main(String[] args) throws Exception {
        String path = args.length >= 1 ? args[0] : System.getProperty("log.path", System.getenv().getOrDefault("LOG_PATH", "logs/telemetry.csv"));
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : Integer.parseInt(System.getProperty("udp.port", System.getenv().getOrDefault("UDP_PORT", "5800")));

        boolean enableNt = args.length >= 3 ? Boolean.parseBoolean(args[2]) : Boolean.parseBoolean(System.getProperty("enable.nt", System.getenv().getOrDefault("ENABLE_NT", "false")));
        String ntTable = args.length >= 4 ? args[3] : System.getProperty("nt.table", System.getenv().getOrDefault("NT_TABLE", "Telemetry"));
        String ntMotors = args.length >= 5 ? args[4] : System.getProperty("nt.motors", System.getenv().getOrDefault("NT_MOTORS", ""));
        int ntInterval = args.length >= 6 ? Integer.parseInt(args[5]) : Integer.parseInt(System.getProperty("nt.interval.ms", System.getenv().getOrDefault("NT_INTERVAL_MS", "1000")));

        final CsvFileLogger logger = new CsvFileLogger(Paths.get(path));

        List<AutoCloseable> components = new ArrayList<>();
        components.add(logger);

        UdpReceiver udp = new UdpReceiver(logger, port);
        components.add(udp);

        NetworkTableMonitor ntm = null;
        if (enableNt) {
            ntm = new NetworkTableMonitor(logger, System.getProperty("nt.server", System.getenv().getOrDefault("NT_SERVER", "")), ntTable, ntMotors, ntInterval);
            components.add(ntm);
            System.out.println("NetworkTableMonitor started (table=" + ntTable + ", motors=" + ntMotors + ")");
        }

        CountDownLatch stopLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown requested, closing receiver components...");
            for (AutoCloseable c : components) {
                try { c.close(); } catch (Exception e) { e.printStackTrace(); }
            }
            stopLatch.countDown();
        }));

        System.out.println("Receiver running. Writing to: " + path + " Listening on UDP port: " + port + (enableNt ? " (NetworkTables enabled)" : ""));
        stopLatch.await();
    }
}
