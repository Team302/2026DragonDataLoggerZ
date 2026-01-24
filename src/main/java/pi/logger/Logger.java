package pi.logger;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dynamic CSV logger for telemetry data.
 * Writes a header on first log (combining "timestamp" + field names),
 * then appends CSV rows for each LogRecord. Uses ISO-8601 instant for timestamps.
 * Supports any field names and values.
 * 
 * Thread-safe via synchronized lock.
 */
public class Logger implements Closeable {
    private final BufferedWriter writer;
    private final Object lock = new Object();
    private final DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT;
    private boolean headerWritten = false;
    private List<String> headerFields = null;

    /**
     * Creates a new Logger that writes to the specified file.
     * Auto-creates parent directories if they don't exist.
     * 
     * @param file Path to the CSV file
     * @throws IOException if file creation fails
     */
    public Logger(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        boolean exists = Files.exists(file);
        writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (exists && file.toFile().length() > 0) {
            // File already has content, assume header is written
            headerWritten = true;
        }
    }

    /**
     * Logs a telemetry record to the CSV file.
     * On first call, writes a header row with field names.
     * Thread-safe via synchronized lock.
     * 
     * @param record The telemetry record to log
     * @throws IOException if write fails
     */
    public void log(UdpReceiver.LogRecord record) throws IOException {
        synchronized (lock) {
            // On first record, write header
            if (!headerWritten) {
                List<String> fields = new ArrayList<>(record.getFields().keySet());
                headerFields = fields;
                
                StringBuilder header = new StringBuilder("timestamp");
                for (String field : fields) {
                    header.append(",").append(field);
                }
                header.append("\n");
                writer.write(header.toString());
                writer.flush();
                headerWritten = true;
            }

            // Write data row
            StringBuilder line = new StringBuilder(iso.format(record.getTimestamp()));
            Map<String, String> fields = record.getFields();
            for (String fieldName : headerFields) {
                String value = fields.getOrDefault(fieldName, "");
                line.append(",").append(value);
            }
            line.append("\n");
            writer.write(line.toString());
            writer.flush();
        }
    }

    /**
     * Closes the CSV file and flushes all pending writes.
     * 
     * @throws IOException if close fails
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }
}
