package pi.logger;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.util.datalog.DataLogWriter;

public final class USBFileLogger {

    private static final long MAX_FILE_AGE_MS = 5 * 60 * 1000; // 5 minutes

    private static final File LOG_DIR = new File("/mnt/usb_logs");

    private static volatile boolean running = true;

    private static DataLogWriter dataLog;
    private static File currentFile;
    private static long fileStartTime;

    // Cache of entry IDs by entry name
    private static final Map<String, Integer> entryIds = new HashMap<>();

    private USBFileLogger() {}

    public static void start() {
        Thread t = new Thread(USBFileLogger::run, "file-logger");
        t.setDaemon(true);
        t.start();
    }

    public static void stop() {
        running = false;
    }

    private static void run() {
        try {
            Files.createDirectories(LOG_DIR.toPath());
            openNewFile();

            BlockingQueue<UdpMessage> queue = UdpReceiver.getQueue();

            while (running) {
                UdpMessage msg = queue.take();
                writeMessage(msg);

                if (shouldRotate()) {
                    rotate();
                }
            }
        } catch (Exception e) {
            System.err.println("FileLogger error");
            e.printStackTrace();
        } finally {
            closeQuietly();
        }
    }

    private static void writeMessage(UdpMessage msg) {
        try {
            String payload = new String(msg.payload());
            
            // Parse CSV format: timestamp,signalID,type,value,units
            String[] parts = payload.split(",", 5);
            if (parts.length < 4) {
                System.err.println("Invalid message format: " + payload);
                return;
            }

            long timestampUs = msg.timestampNs() / 1000;
            String signalID = parts[1].trim();
            String type = parts[2].trim();
            String value = parts[3].trim();
            String units = parts.length > 4 ? parts[4].trim() : "";

            // Create entry key with units if present
            String entryName = units.isEmpty() ? signalID : signalID + " (" + units + ")";

            // Log based on type
            switch (type.toLowerCase()) {
                case "double", "float" -> {
                    try {
                        double doubleValue = Double.parseDouble(value);
                        int entryId = entryIds.computeIfAbsent(
                            entryName,
                            k -> dataLog.start(k, "double")
                        );
                        dataLog.appendDouble(entryId, doubleValue, timestampUs);
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse double: " + value);
                    }
                }
                case "int", "integer", "long" -> {
                    try {
                        long intValue = Long.parseLong(value);
                        int entryId = entryIds.computeIfAbsent(
                            entryName,
                            k -> dataLog.start(k, "int64")
                        );
                        dataLog.appendInteger(entryId, intValue, timestampUs);
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse integer: " + value);
                    }
                }
                case "bool", "boolean" -> {
                    boolean boolValue = Boolean.parseBoolean(value);
                    int entryId = entryIds.computeIfAbsent(
                        entryName,
                        k -> dataLog.start(k, "boolean")
                    );
                    dataLog.appendBoolean(entryId, boolValue, timestampUs);
                }
                case "string", "str" -> {
                    int entryId = entryIds.computeIfAbsent(
                        entryName,
                        k -> dataLog.start(k, "string")
                    );
                    dataLog.appendString(entryId, value, timestampUs);
                }
                default -> {
                    System.err.println("Unknown type '" + type + "' for entry '" + entryName + "'. Defaulting to string.");
                    // Default to string for unknown types
                    int entryId = entryIds.computeIfAbsent(
                        entryName,
                        k -> dataLog.start(k, "string")
                    );
                    dataLog.appendString(entryId, value, timestampUs);
                }
            }

            // Flush periodically
            dataLog.flush();

        } catch (Exception e) {
            System.err.println("Error writing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean shouldRotate() {
        long age = System.currentTimeMillis() - fileStartTime;
        return age >= MAX_FILE_AGE_MS;
    }

    private static void rotate() {
        closeQuietly();
        clearEntryCache();
        openNewFile();
    }

    private static void openNewFile() {
        try {
            String name = "udp_" + timestamp() + ".wpilog";
            currentFile = new File(LOG_DIR, name);

            dataLog = new DataLogWriter(currentFile.getAbsolutePath());

            fileStartTime = System.currentTimeMillis();

            System.out.println("Logging to " + currentFile.getName());
        } catch (Exception e) {
            System.err.println("Failed to open log file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void clearEntryCache() {
        entryIds.clear();
    }

    private static void closeQuietly() {
        try {
            if (dataLog != null) {
                dataLog.flush();
                dataLog.close();
            }
        } catch (Exception ignored) {}
    }

    private static String timestamp() {
        return DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }
}

