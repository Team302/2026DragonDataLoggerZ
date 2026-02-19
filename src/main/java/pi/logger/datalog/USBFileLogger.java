//====================================================================================================================================================
// Copyright 2026 Lake Orion Robotics FIRST Team 302
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
// OR OTHER DEALINGS IN THE SOFTWARE.
//====================================================================================================================================================
package pi.logger.datalog;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.math.geometry.Pose2d;
import pi.logger.structs.ChassisSpeeds;
import pi.logger.udp.UdpMessage;
import pi.logger.udp.UdpReceiver;
import edu.wpi.first.util.datalog.DataLogWriter;
import edu.wpi.first.util.datalog.StructLogEntry;
import edu.wpi.first.util.datalog.StructArrayLogEntry;
import edu.wpi.first.util.struct.Struct;

public final class USBFileLogger {

    private static final long MAX_FILE_AGE_MS = 5 * 60 * 1000; // 5 minutes

    private static final File LOG_DIR = new File("/mnt/usb_logs");

    private static volatile boolean running = true;

    private static DataLogWriter dataLog;
    private static File currentFile;
    private static long fileStartTime;

    // Cache of entry IDs by entry name
    private static final Map<String, Integer> entryIds = new HashMap<>();
    
    // Cache of struct log entries (support multiple struct types)
    private static final Map<String, StructLogEntry<?>> structEntries = new HashMap<>();
    private static final Map<String, StructArrayLogEntry<?>> structArrayEntries = new HashMap<>();

    private USBFileLogger() {}

    public static void start() {
        Thread t = new Thread(USBFileLogger::run, "file-logger");
        t.setDaemon(true);
        t.start();
    }

    public static void stop() {
        running = false;
    }

    /**
     * Log a double value from an external source (e.g., NetworkTables)
     */
    public static void logDouble(String name, double value) {
        if (dataLog == null) return;
        
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(
                name,
                k -> dataLog.start(k, "double")
            );
            dataLog.appendDouble(entryId, value, 0);
        }
    }

    /**
     * Log an integer value from an external source
     */
    public static void logInteger(String name, long value) {
        if (dataLog == null) return;

        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(
                name,
                k -> dataLog.start(k, "int64")
            );
            dataLog.appendInteger(entryId, value, 0);
        }
    }

    /**
     * Log a boolean value from an external source
     */
    public static void logBoolean(String name, boolean value) {
        if (dataLog == null) return;
        
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(
                name,
                k -> dataLog.start(k, "boolean")
            );
            dataLog.appendBoolean(entryId, value, 0);
        }
    }

    /**
     * Log a string value from an external source
     */
    public static void logString(String name, String value) {
        if (dataLog == null) return;
        
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(
                name,
                k -> dataLog.start(k, "string")
            );
            dataLog.appendString(entryId, value, 0);
        }
    }

    /**
     * Log a Pose2d struct from an external source (e.g., NetworkTables)
     */
    public static void logStruct(String name, Pose2d value) {
        if (dataLog == null) return;
        synchronized (structEntries) {
            @SuppressWarnings("unchecked")
            StructLogEntry<Pose2d> entry = (StructLogEntry<Pose2d>) structEntries.computeIfAbsent(
                name,
                k -> StructLogEntry.create(dataLog, k, Pose2d.struct)
            );
            entry.append(value, 0);
            dataLog.flush();
        }
    }

    /**
     * Log a ChassisSpeeds struct
     */
    public static void logStruct(String name, ChassisSpeeds value) {
        if (dataLog == null) return;

        synchronized (structEntries) {
            @SuppressWarnings("unchecked")
            StructLogEntry<ChassisSpeeds> entry = (StructLogEntry<ChassisSpeeds>) structEntries.computeIfAbsent(
                name,
                k -> StructLogEntry.create(dataLog, k, ChassisSpeeds.struct)
            );
            entry.append(value, 0);
            dataLog.flush();
        }
    }

    /**
     * Log a SwerveModulePosition struct
     */
    public static void logStruct(String name, pi.logger.structs.SwerveModulePosition value) {
        if (dataLog == null) return;

        synchronized (structEntries) {
            @SuppressWarnings("unchecked")
            StructLogEntry<pi.logger.structs.SwerveModulePosition> entry = (StructLogEntry<pi.logger.structs.SwerveModulePosition>) structEntries.computeIfAbsent(
                name,
                k -> StructLogEntry.create(dataLog, k, pi.logger.structs.SwerveModulePosition.struct)
            );
            entry.append(value, 0);
            dataLog.flush();
        }
    }

    /**
     * Log a SwerveModuleState struct
     */
    public static void logStruct(String name, pi.logger.structs.SwerveModuleState value) {
        if (dataLog == null) return;

        synchronized (structEntries) {
            @SuppressWarnings("unchecked")
            StructLogEntry<pi.logger.structs.SwerveModuleState> entry = (StructLogEntry<pi.logger.structs.SwerveModuleState>) structEntries.computeIfAbsent(
                name,
                k -> StructLogEntry.create(dataLog, k, pi.logger.structs.SwerveModuleState.struct)
            );
            entry.append(value, 0);
            dataLog.flush();
        }
    }

    public static <T> void logStructArray(String name, T[] values, Struct<T> elementStruct) {
        if (dataLog == null || values == null) return;

        synchronized (structArrayEntries) {
            @SuppressWarnings("unchecked")
            StructArrayLogEntry<T> entry = (StructArrayLogEntry<T>) structArrayEntries.computeIfAbsent(
                name,
                k -> StructArrayLogEntry.create(dataLog, k, elementStruct)
            );
            entry.append(values, 0);
            dataLog.flush();
        }
    }

    /**
     * Flush the log to disk
     */
    public static void flush() {
        if (dataLog != null) {
            dataLog.flush();
        }
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
                        dataLog.appendDouble(entryId, doubleValue, 0);
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
                        dataLog.appendInteger(entryId, intValue, 0);
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
                    dataLog.appendBoolean(entryId, boolValue, 0);
                }
                case "string", "str" -> {
                    int entryId = entryIds.computeIfAbsent(
                        entryName,
                        k -> dataLog.start(k, "string")
                    );
                    dataLog.appendString(entryId, value, 0);
                }
                default -> {
                    System.err.println("Unknown type '" + type + "' for entry '" + entryName + "'. Defaulting to string.");
                    // Default to string for unknown types
                    int entryId = entryIds.computeIfAbsent(
                        entryName,
                        k -> dataLog.start(k, "string")
                    );
                    dataLog.appendString(entryId, value, 0);
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

            // Write a start-time entry at timestamp 0. Populate it with the
            // current epoch microseconds so viewers that compute relative time
            // (entry_time - start_time) will show 0 at the beginning of the file.
            try {
                long startEpochMicros = System.currentTimeMillis() * 1000L;
                int startEntryId = dataLog.start(".startTime", "int64");
                dataLog.appendInteger(startEntryId, startEpochMicros, 0L);
                dataLog.flush();
            } catch (Exception ignored) {}

            fileStartTime = System.currentTimeMillis();

            System.out.println("Logging to " + currentFile.getName());
        } catch (Exception e) {
            System.err.println("Failed to open log file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void clearEntryCache() {
        entryIds.clear();
        structEntries.clear();
        structArrayEntries.clear();
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

