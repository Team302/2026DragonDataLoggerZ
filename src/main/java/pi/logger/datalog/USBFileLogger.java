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
import edu.wpi.first.util.datalog.DataLogWriter;
import edu.wpi.first.util.datalog.StructLogEntry;
import edu.wpi.first.util.datalog.StructArrayLogEntry;
import edu.wpi.first.util.struct.Struct;
import pi.logger.config.LoggerConfig;
import pi.logger.utils.TimeUtils;

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
        // Initialize the relative time clock so all log timestamps start near 0
        TimeUtils.initialize();
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

    public static void logDouble(String name, double value, long timestampUs) {
        if (dataLog == null) return;
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(name, k -> dataLog.start(k, "double", "", timestampUs));
            dataLog.appendDouble(entryId, value, timestampUs);
            recordWriteAndMaybeFlush();
        }
    }

    public static void logInteger(String name, long value, long timestampUs) {
        if (dataLog == null) return;
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(name, k -> dataLog.start(k, "int64", "", timestampUs));
            dataLog.appendInteger(entryId, value, timestampUs);
            recordWriteAndMaybeFlush();
        }
    }

    public static void logBoolean(String name, boolean value, long timestampUs) {
        if (dataLog == null) return;
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(name, k -> dataLog.start(k, "boolean", "", timestampUs));
            dataLog.appendBoolean(entryId, value, timestampUs);
            recordWriteAndMaybeFlush();
        }
    }

    public static void logString(String name, String value, long timestampUs) {
        if (dataLog == null) return;
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(name, k -> dataLog.start(k, "string", "", timestampUs));
            dataLog.appendString(entryId, value, timestampUs);
            recordWriteAndMaybeFlush();
        }
    }

    public static void logBooleanArray(String name, boolean[] values, long timestampUs) {
        if (dataLog == null) return;
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(name, k -> dataLog.start(k, "boolean[]", "", timestampUs));
            dataLog.appendBooleanArray(entryId, values, timestampUs);
            recordWriteAndMaybeFlush();
        }
    }

    public static void logDoubleArray(String name, double[] values, long timestampUs) {
        if (dataLog == null) return;
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(name, k -> dataLog.start(k, "double[]", "", timestampUs));
            dataLog.appendDoubleArray(entryId, values, timestampUs);
            recordWriteAndMaybeFlush();
        }
    }

    public static void logIntegerArray(String name, long[] values, long timestampUs) {
        if (dataLog == null) return;
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(name, k -> dataLog.start(k, "int64[]", "", timestampUs));
            dataLog.appendIntegerArray(entryId, values, timestampUs);
            recordWriteAndMaybeFlush();
        }
    }

    public static void logFloatArray(String name, float[] values, long timestampUs) {
        if (dataLog == null) return;
        synchronized (entryIds) {
            int entryId = entryIds.computeIfAbsent(name, k -> dataLog.start(k, "float[]", "", timestampUs));
            dataLog.appendFloatArray(entryId, values, timestampUs);
            recordWriteAndMaybeFlush();
        }
    }

    public static <T> void logStructEntry(String name, T value, Struct<T> struct) {
        logStructEntry(name, value, struct, TimeUtils.nowUs());
    }

    public static <T> void logStructEntry(String name, T value, Struct<T> struct, long timestampUs) {
        if (dataLog == null || value == null || struct == null) return;
        synchronized (structEntries) {
            @SuppressWarnings("unchecked")
            StructLogEntry<T> entry = (StructLogEntry<T>) structEntries.computeIfAbsent(
                name, k -> StructLogEntry.create(dataLog, k, struct, timestampUs)
            );
            entry.append(value, timestampUs);
            recordWriteAndMaybeFlush();
        }
    }

    public static <T> void logStructArray(String name, T[] values, Struct<T> elementStruct, long timestampUs) {
        if (dataLog == null || values == null) return;
        synchronized (structArrayEntries) {
            @SuppressWarnings("unchecked")
            StructArrayLogEntry<T> entry = (StructArrayLogEntry<T>) structArrayEntries.computeIfAbsent(
                name, k -> StructArrayLogEntry.create(dataLog, k, elementStruct, timestampUs)
            );
            entry.append(values, timestampUs);
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

