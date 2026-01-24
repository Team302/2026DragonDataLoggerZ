package pi.logger;

/**
 * Simple logging interface for the Pi data logger.
 * Implementations should be thread-safe where needed.
 */
public interface Logger {
    /**
     * Log a record. Implementations may throw IOException at runtime.
     */
    void log(LogRecord record) throws Exception;

    /**
     * Close and flush any resources.
     */
    void close() throws Exception;
}
