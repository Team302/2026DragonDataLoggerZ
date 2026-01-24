package pi.logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPOutputStream;

public final class USBFileLogger {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB
    private static final long MAX_FILE_AGE_MS = 5 * 60 * 1000; // 5 minutes

    private static final File LOG_DIR = new File("logs");

    private static volatile boolean running = true;

    private static BufferedOutputStream out;
    private static File currentFile;
    private static long bytesWritten;
    private static long fileStartTime;

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

    private static void writeMessage(UdpMessage msg) throws IOException {
        byte[] payload = msg.payload();

        // Record format:
        // [timestamp_ns][payload_len][payload]
        ByteBuffer header = ByteBuffer.allocate(16);
        header.putLong(msg.timestampNs());
        header.putInt(payload.length);
        header.putInt(0); // reserved / future flags

        out.write(header.array());
        out.write(payload);

        bytesWritten += header.capacity() + payload.length;
    }

    private static boolean shouldRotate() {
        long age = System.currentTimeMillis() - fileStartTime;
        return bytesWritten >= MAX_FILE_SIZE_BYTES || age >= MAX_FILE_AGE_MS;
    }

    private static void rotate() throws IOException {
        closeQuietly();
        compress(currentFile);
        openNewFile();
    }

    private static void openNewFile() throws IOException {
        String name = "udp_" + timestamp() + ".log";
        currentFile = new File(LOG_DIR, name);

        out = new BufferedOutputStream(
                new FileOutputStream(currentFile),
                1 << 20 // 1 MB buffer
        );

        bytesWritten = 0;
        fileStartTime = System.currentTimeMillis();

        System.out.println("Logging to " + currentFile.getName());
    }

    private static void closeQuietly() {
        try {
            if (out != null) {
                out.flush();
                out.close();
            }
        } catch (IOException ignored) {}
    }

    private static void compress(File file) {
        File gz = new File(file.getAbsolutePath() + ".gz");

        try (GZIPOutputStream gos =
                     new GZIPOutputStream(
                             new BufferedOutputStream(
                                     new FileOutputStream(gz), 1 << 20));
             FileOutputStream fis =
                     new FileOutputStream(file)) {

            Files.copy(file.toPath(), gos);
            Files.delete(file.toPath());

            System.out.println("Compressed " + gz.getName());

        } catch (IOException e) {
            System.err.println("Failed to compress " + file.getName());
            e.printStackTrace();
        }
    }

    private static String timestamp() {
        return DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }
}

