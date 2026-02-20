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
import edu.wpi.first.math.geometry.Pose2d;
import pi.logger.structs.ChassisSpeeds;
import edu.wpi.first.util.datalog.DataLogWriter;
import edu.wpi.first.util.datalog.StructLogEntry;
import edu.wpi.first.util.datalog.StructArrayLogEntry;
import edu.wpi.first.util.struct.Struct;
import pi.logger.config.LoggerConfig;

public final class USBFileLogger {

    private static final long DEFAULT_MAX_FILE_AGE_MS = 5 * 60 * 1000; // 5 minutes
    private static final int DEFAULT_FLUSH_ENTRY_THRESHOLD = 200;
    private static final long DEFAULT_FLUSH_TIME_THRESHOLD_MS = 500;
    private static final String DEFAULT_LOG_DIR = "/mnt/usb_logs";

    private static final long maxFileAgeMs = LoggerConfig.getLong("logger.maxFileAgeMs", DEFAULT_MAX_FILE_AGE_MS, 1);
    private static final int flushEntryThreshold = LoggerConfig.getInt("logger.flushEntryThreshold", DEFAULT_FLUSH_ENTRY_THRESHOLD, 1, Integer.MAX_VALUE);
    private static final long flushTimeThresholdMs = LoggerConfig.getLong("logger.flushTimeThresholdMs", DEFAULT_FLUSH_TIME_THRESHOLD_MS, 1);

    private static final File LOG_DIR = new File(LoggerConfig.getString("logger.logDir", DEFAULT_LOG_DIR));

    private static volatile boolean running = true;
    private static Thread rotationThread;

    private static DataLogWriter dataLog;
    private static File currentFile;
    private static long fileStartTime;
    private static final Object flushLock = new Object();
    private static long writesSinceFlush = 0;
    private static long lastFlushTimeMs = System.currentTimeMillis();

    // Cache of entry IDs by entry name
    private static final Map<String, Integer> entryIds = new HashMap<>();
    
    // Cache of struct log entries (support multiple struct types)
    private static final Map<String, StructLogEntry<?>> structEntries = new HashMap<>();
    private static final Map<String, StructArrayLogEntry<?>> structArrayEntries = new HashMap<>();
    private USBFileLogger() {}

    public static void start() {
        System.out.println("USBFileLogger config: maxFileAgeMs=" + maxFileAgeMs
        + ", flushEntryThreshold=" + flushEntryThreshold
        + ", flushTimeThresholdMs=" + flushTimeThresholdMs);
        Thread t = new Thread(USBFileLogger::run, "file-logger");
        t.setDaemon(true);
        rotationThread = t;
        t.start();
    }

    public static void stop() {
        running = false;
        Thread t = rotationThread;
        if (t != null) {
            t.interrupt();
        }
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
            recordWriteAndMaybeFlush();
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
            recordWriteAndMaybeFlush();
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
            recordWriteAndMaybeFlush();
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
            recordWriteAndMaybeFlush();
        }
    }

    /**
     * Log a Pose2d struct from an external source (e.g., NetworkTables)
     */
    public static void logStruct(String name, Pose2d value) {
        logStructEntry(name, value, Pose2d.struct);
    }

    /**
     * Convert a double array {@code [x, y, rotationRadians]} to a {@link Pose2d} and log it as a struct.
     *
     * @param name  the log entry name
     * @param array double array containing {@code [x, y, rotationRadians]}
     * @throws IllegalArgumentException if the array is {@code null} or has fewer than 3 elements
     */
    public static void logPose2d(String name, double[] array) {
        logStruct(name, Pose2dUtil.fromArray(array));
    }

    /**
     * Convert a double array {@code [x, y, rotation]} to a {@link Pose2d} and log it as a struct.
     *
     * @param name         the log entry name
     * @param array        double array containing {@code [x, y, rotation]}
     * @param rotInDegrees {@code true} if the rotation value is in degrees; {@code false} for radians
     * @throws IllegalArgumentException if the array is {@code null} or has fewer than 3 elements
     */
    public static void logPose2d(String name, double[] array, boolean rotInDegrees) {
        logStruct(name, Pose2dUtil.fromArray(array, rotInDegrees));
    }

    /**
     * Log a ChassisSpeeds struct
     */
    public static void logStruct(String name, ChassisSpeeds value) {
        logStructEntry(name, value, ChassisSpeeds.struct);
    }

    /**
     * Log a SwerveModulePosition struct
     */
    public static void logStruct(String name, pi.logger.structs.SwerveModulePosition value) {
        logStructEntry(name, value, pi.logger.structs.SwerveModulePosition.struct);
    }

    /**
     * Log a SwerveModuleState struct
     */
    public static void logStruct(String name, pi.logger.structs.SwerveModuleState value) {
        logStructEntry(name, value, pi.logger.structs.SwerveModuleState.struct);
    }

    public static <T> void logStructEntry(String name, T value, Struct<T> struct) {
        if (dataLog == null || value == null || struct == null) return;

        synchronized (structEntries) {
            @SuppressWarnings("unchecked")
            StructLogEntry<T> entry = (StructLogEntry<T>) structEntries.computeIfAbsent(
                name,
                k -> StructLogEntry.create(dataLog, k, struct)
            );
            entry.append(value, 0);
            recordWriteAndMaybeFlush();
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
            recordWriteAndMaybeFlush();
        }
    }

    /**
     * Flush the log to disk
     */
    public static void flush() {
        DataLogWriter log = dataLog;
        if (log != null) {
            synchronized (flushLock) {
                try {
                    log.flush();
                } catch (Exception e) {
                    System.err.println("USBFileLogger flush failed: " + e.getMessage());
                }
                writesSinceFlush = 0;
                lastFlushTimeMs = System.currentTimeMillis();
            }
        }
    }

    private static void run() {
        try {
            Files.createDirectories(LOG_DIR.toPath());
            openNewFile();

            while (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
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

    public static void logCsvPayload(String payload) {
        if (payload == null || dataLog == null) {
            return;
        }

        try {
            
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
                        logDouble(entryName, doubleValue);
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse double: " + value);
                    }
                }
                case "int", "integer", "long" -> {
                    try {
                        long intValue = Long.parseLong(value);
                        logInteger(entryName, intValue);
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse integer: " + value);
                    }
                }
                case "bool", "boolean" -> logBoolean(entryName, Boolean.parseBoolean(value));
                case "string", "str" -> logString(entryName, value);
                case "pose2d" -> {
                    // Expected value format: "x;y;rot"
                    // Rotation is in degrees when units contains "deg", otherwise radians.
                    try {
                        String[] coords = value.split(";", 3);
                        if (coords.length < 3) {
                            System.err.println("Invalid Pose2d value format (expected 'x;y;rot'): " + value);
                            return;
                        }
                        double x = Double.parseDouble(coords[0].trim());
                        double y = Double.parseDouble(coords[1].trim());
                        double rot = Double.parseDouble(coords[2].trim());
                        boolean rotInDegrees = units.toLowerCase().contains("deg");
                        logPose2d(entryName, new double[]{x, y, rot}, rotInDegrees);
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse Pose2d: " + value);
                    }
                }
                default -> {
                    System.err.println("Unknown type '" + type + "' for entry '" + entryName + "'. Defaulting to string.");
                    // Default to string for unknown types
                    logString(entryName, value);
                }
            }

        } catch (Exception e) {
            System.err.println("Error writing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean shouldRotate() {
        long age = System.currentTimeMillis() - fileStartTime;
        return age >= maxFileAgeMs;
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
            resetFlushState();

            // Write a start-time entry at timestamp 0. Populate it with the
            // current epoch microseconds so viewers that compute relative time
            // (entry_time - start_time) will show 0 at the beginning of the file.
            try {
                long startEpochMicros = System.currentTimeMillis() * 1000L;
                int startEntryId = dataLog.start(".startTime", "int64");
                dataLog.appendInteger(startEntryId, startEpochMicros, 0L);
                flush();
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
                flush();
                dataLog.close();
            }
        } catch (Exception ignored) {}
    }

    private static void recordWriteAndMaybeFlush() {
        DataLogWriter log = dataLog;
        if (log == null) {
            return;
        }

        synchronized (flushLock) {
            writesSinceFlush++;
            long now = System.currentTimeMillis();
            if (writesSinceFlush >= flushEntryThreshold ||
                    (now - lastFlushTimeMs) >= flushTimeThresholdMs) {
                try {
                    log.flush();
                } catch (Exception e) {
                    System.err.println("USBFileLogger flush failed: " + e.getMessage());
                }
                writesSinceFlush = 0;
                lastFlushTimeMs = now;
            }
        }
    }

    private static void resetFlushState() {
        synchronized (flushLock) {
            writesSinceFlush = 0;
            lastFlushTimeMs = System.currentTimeMillis();
        }
    }

    private static String timestamp() {
        return DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }

}

