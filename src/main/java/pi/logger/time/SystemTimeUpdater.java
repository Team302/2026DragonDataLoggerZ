package pi.logger.time;

import edu.wpi.first.networktables.DoubleSubscriber;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pi.logger.nt.NtClient;

import java.io.IOException;
import java.util.EnumSet;

public class SystemTimeUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(SystemTimeUpdater.class);

    private static DoubleSubscriber timeSubscriber;

    public static void start() {
        Thread updaterThread = new Thread(() -> {
            LOG.info("SystemTimeUpdater thread started");
            try {
                NetworkTableInstance inst = NtClient.get();
                LOG.info("Got NetworkTableInstance");
                NetworkTable table = inst.getTable("pi-logger");
                LOG.info("Got NetworkTable: pi-logger");

                timeSubscriber = table.getDoubleTopic("pi-system-time").subscribe(0.0);
                LOG.info("Subscribed to pi-system-time topic");
                
                // Handle immediate value if already published
                double initialValue = timeSubscriber.get(0.0);
                LOG.info("Initial value retrieved: {}", initialValue);
                if (initialValue > 0) {
                    LOG.info("Initial value > 0, calling updateSystemTime");
                    updateSystemTime(initialValue);
                } else {
                    LOG.info("Initial value is 0 or negative, waiting for listener events");
                }

                inst.addListener(
                    table.getEntry("pi-system-time"),
                    EnumSet.of(NetworkTableEvent.Kind.kValueAll),
                    event -> {
                        LOG.info("Listener event fired for pi-system-time");
                        double epochSeconds = timeSubscriber.get(0.0);
                        LOG.info("Event value retrieved: {}", epochSeconds);
                        if (epochSeconds > 0) {
                            LOG.info("Event value > 0, calling updateSystemTime");
                            updateSystemTime(epochSeconds);
                        } else {
                            LOG.info("Event value is 0 or negative, skipping updateSystemTime");
                        }
                    }
                );
                LOG.info("Listener registered successfully");
                LOG.info("Started SystemTimeUpdater listening on pi-system-time");
            } catch (Exception e) {
                LOG.error("Error in SystemTimeUpdater thread", e);
            }
        }, "SystemTimeUpdater");
        updaterThread.setDaemon(true);
        updaterThread.start();
        LOG.info("SystemTimeUpdater thread started in background");
    }

    private static void updateSystemTime(double epochSeconds) {
        long epochMillis = (long) (epochSeconds * 1000);
        
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            LOG.info("System time update skipped on Windows. Would have updated to {} ms", epochMillis);
            return;
        }

        LOG.info("Attempting to update system time to {} ms", epochMillis);

        try {
            // Using date to safely update system time on a Unix system
            // Format: "date -s @seconds"
            ProcessBuilder pb = new ProcessBuilder("sudo", "date", "-s", String.format("@%d", (long) epochSeconds));
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                LOG.info("Successfully updated system time via date command");
            } else {
                LOG.warn("Failed to update system time, exit code: {}", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("Error updating system time", e);
        }
    }
}
