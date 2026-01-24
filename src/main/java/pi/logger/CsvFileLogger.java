package pi.logger;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

/**
 * Simple CSV logger. Creates parent directories, writes a header once, and appends
 * CSV lines for each LogRecord. Uses ISO-8601 instant for timestamps.
 */
public class CsvFileLogger implements Logger, Closeable {
    private final BufferedWriter writer;
    private final Object lock = new Object();
    private final DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT;

    public CsvFileLogger(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        boolean exists = Files.exists(file);
        writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (!exists) {
            writer.write("timestamp,voltage,motorId,motorSpeed,motorCurrent\n");
            writer.flush();
        }
    }

    @Override
    public void log(LogRecord record) throws IOException {
        String line = String.format("%s,%.6f,%s,%.6f,%.6f\n",
                iso.format(record.getTimestamp()),
                record.getVoltage(),
                record.getMotorId(),
                record.getMotorSpeed(),
                record.getMotorCurrent());
        synchronized (lock) {
            writer.write(line);
            writer.flush();
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
