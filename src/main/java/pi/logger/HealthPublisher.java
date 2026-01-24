package pi.logger;

import edu.wpi.first.networktables.*;

import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class HealthPublisher {

    private HealthPublisher() {}

    public static void start() {
        NetworkTable table =
                NtClient.get().getTable("pi");

        NetworkTableEntry connected =
                table.getEntry("connected");
        NetworkTableEntry queueDepth =
                table.getEntry("logQueueDepth");
        NetworkTableEntry diskFree =
                table.getEntry("diskFreeMB");
        NetworkTableEntry cpuLoad =
                table.getEntry("cpuLoad");

        connected.setBoolean(true);

        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-publisher");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(() -> {
            try {
                queueDepth.setInteger(
                        UdpReceiver.getQueue().size());

                diskFree.setDouble(getDiskFreeMB("."));

                cpuLoad.setDouble(getCpuLoad());

            } catch (Exception ignored) {}
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static double getDiskFreeMB(String path) throws Exception {
        FileStore store = Files.getFileStore(Path.of(path));
        return store.getUsableSpace() / 1e6;
    }

    private static double getCpuLoad() {
        var os =
                (com.sun.management.OperatingSystemMXBean)
                        ManagementFactory.getOperatingSystemMXBean();

        return os.getSystemCpuLoad(); // 0.0–1.0
    }
}

