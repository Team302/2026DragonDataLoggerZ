package pi.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Standalone receiver main. Starts a Logger and the UdpReceiver.
 * Supports logging to external USB storage.
 * 
 * Usage:
 *   java pi.logger.ReceiverMain [logPath] [udpPort]
 *   java pi.logger.ReceiverMain --usb [udpPort]                  (auto-detect first USB)
 *   java pi.logger.ReceiverMain --usb-label <label> [udpPort]    (find USB by label)
 *   LOG_PATH=/path/to/usb/logs.csv UDP_PORT=5800 java pi.logger.ReceiverMain
 * 
 * Environment variables (precedence over args):
 *   LOG_PATH: override log file path
 *   USB_PATH: use this USB mount path for logging
 *   UDP_PORT: override UDP port
 */
public class ReceiverMain {

    /**
     * Represents a detected USB storage device.
     */
    private static class UsbDevice {
        final String mountPath;
        final String label;
        final long totalSpace;
        final long freeSpace;

        UsbDevice(String mountPath, String label, long totalSpace, long freeSpace) {
            this.mountPath = mountPath;
            this.label = label;
            this.totalSpace = totalSpace;
            this.freeSpace = freeSpace;
        }

        @Override
        public String toString() {
            return String.format("UsbDevice{path=%s, label=%s, total=%d MB, free=%d MB}",
                    mountPath, label, totalSpace / (1024 * 1024), freeSpace / (1024 * 1024));
        }
    }

    /**
     * List all available USB storage devices by scanning common mount directories.
     * On Raspberry Pi Linux, checks /media and /mnt.
     * Returns empty list if no USB devices found or running on non-Linux.
     */
    private static List<UsbDevice> listUsbDevices() {
        List<UsbDevice> devices = new ArrayList<>();
        String osName = System.getProperty("os.name", "").toLowerCase();

        // Only check for USB devices on Linux
        if (!osName.contains("linux")) {
            System.out.println("[ReceiverMain] Not running on Linux; USB detection skipped.");
            return devices;
        }

        // Check common Pi USB mount points
        String[] checkPaths = {"/media", "/mnt", "/home"};
        for (String checkPath : checkPaths) {
            File dir = new File(checkPath);
            if (dir.exists() && dir.isDirectory()) {
                scanDirectory(dir, devices);
            }
        }

        return devices;
    }

    /**
     * Scan a directory for USB-like mount points and add them to the list.
     * Heuristic: if a subdirectory is writable and has reasonable space, consider it a potential USB.
     */
    private static void scanDirectory(File dir, List<UsbDevice> devices) {
        try {
            File[] files = dir.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.isDirectory() && isLikelyUsbMount(file)) {
                    try {
                        long total = file.getTotalSpace();
                        long free = file.getFreeSpace();
                        String label = file.getName();
                        devices.add(new UsbDevice(file.getAbsolutePath(), label, total, free));
                    } catch (Exception e) {
                        // Skip if we can't get space info
                    }
                }
            }
        } catch (Exception e) {
            // Silently skip directories we can't read
        }
    }

    /**
     * Heuristic to check if a directory is likely a USB mount.
     * Returns true if directory is writable and doesn't match excluded patterns.
     */
    private static boolean isLikelyUsbMount(File dir) {
        try {
            // Must be writable
            if (!dir.canWrite()) {
                return false;
            }

            String name = dir.getName();

            // Exclude common non-USB directories
            if (name.equals("root") || name.equals("home") || name.equals("boot") ||
                    name.equals("var") || name.equals("tmp") || name.startsWith(".")) {
                return false;
            }

            // Check if it has reasonable space (> 100 MB = likely external)
            long free = dir.getFreeSpace();
            return free > 100 * 1024 * 1024;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find the first available USB device with sufficient free space.
     * Returns the mount path, or null if no suitable USB found.
     */
    private static String findFirstUsbPath(long minFreeSpaceMb) {
        List<UsbDevice> devices = listUsbDevices();
        for (UsbDevice dev : devices) {
            if (dev.freeSpace >= minFreeSpaceMb * 1024 * 1024) {
                return dev.mountPath;
            }
        }
        return null;
    }

    /**
     * Find a USB device by label (directory name).
     * Returns the mount path, or null if not found.
     */
    private static String findUsbByLabel(String label) {
        List<UsbDevice> devices = listUsbDevices();
        for (UsbDevice dev : devices) {
            if (dev.label.equalsIgnoreCase(label)) {
                return dev.mountPath;
            }
        }
        return null;
    }

    /**
     * Validate a path: ensure parent directory exists or can be created, and is writable.
     */
    private static boolean validatePath(String path) {
        try {
            Path p = Paths.get(path);
            Path parent = p.getParent();

            if (parent != null) {
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                if (!Files.isWritable(parent)) {
                    System.err.println("[ReceiverMain] Path not writable: " + parent);
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("[ReceiverMain] Error validating path " + path + ": " + e.getMessage());
            return false;
        }
    }
    public static void main(String[] args) throws Exception {
        String path = null;
        int port = 5800;
        boolean useUsb = false;
        String usbLabel = null;

        // Parse arguments
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("--usb")) {
                useUsb = true;
                i++;
                if (i < args.length && !args[i].startsWith("-")) {
                    port = Integer.parseInt(args[i]);
                    i++;
                }
            } else if (arg.equals("--usb-label")) {
                i++;
                if (i < args.length) {
                    usbLabel = args[i];
                    useUsb = true;
                    i++;
                    if (i < args.length && !args[i].startsWith("-")) {
                        port = Integer.parseInt(args[i]);
                        i++;
                    }
                }
            } else if (!arg.startsWith("-")) {
                // Positional arguments: [logPath] [udpPort]
                if (path == null) {
                    path = arg;
                } else {
                    port = Integer.parseInt(arg);
                }
                i++;
            } else {
                i++;
            }
        }

        // Check environment variables (override args)
        String envPath = System.getenv("LOG_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            path = envPath;
        }

        String envUsbPath = System.getenv("USB_PATH");
        if (envUsbPath != null && !envUsbPath.isEmpty()) {
            path = envUsbPath;
            useUsb = false;  // explicit USB_PATH takes precedence
        }

        String envPort = System.getenv("UDP_PORT");
        if (envPort != null && !envPort.isEmpty()) {
            port = Integer.parseInt(envPort);
        }

        // System properties (highest precedence)
        String propPath = System.getProperty("log.path");
        if (propPath != null && !propPath.isEmpty()) {
            path = propPath;
        }

        String propPort = System.getProperty("udp.port");
        if (propPort != null && !propPort.isEmpty()) {
            port = Integer.parseInt(propPort);
        }

        // Handle USB auto-detection
        if (useUsb) {
            if (usbLabel != null) {
                path = findUsbByLabel(usbLabel);
                if (path == null) {
                    System.err.println("USB device with label '" + usbLabel + "' not found.");
                    System.err.println("Available USB devices:");
                    for (UsbDevice dev : listUsbDevices()) {
                        System.err.println("  " + dev);
                    }
                    System.exit(1);
                }
            } else {
                path = findFirstUsbPath(50);  // require 50MB free
                if (path == null) {
                    System.err.println("No USB device found with sufficient free space.");
                    System.err.println("Available USB devices:");
                    for (UsbDevice dev : listUsbDevices()) {
                        System.err.println("  " + dev);
                    }
                    System.exit(1);
                }
            }
            // Append logs subdirectory on USB
            path = path + "/pi-logger/telemetry.csv";
        }

        // Fallback to default local path if nothing specified
        if (path == null || path.isEmpty()) {
            path = "logs/telemetry.csv";
        }

        // Validate the path is writable
        if (!validatePath(path)) {
            System.err.println("Cannot write to path: " + path);
            System.exit(1);
        }

        final Logger logger = new Logger(Paths.get(path));

        List<AutoCloseable> components = new ArrayList<>();
        components.add(logger);

        UdpReceiver udp = new UdpReceiver(logger, port);
        components.add(udp);

        CountDownLatch stopLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown requested, closing receiver components...");
            for (AutoCloseable c : components) {
                try { c.close(); } catch (Exception e) { e.printStackTrace(); }
            }
            stopLatch.countDown();
        }));

        System.out.println("Receiver running. Writing to: " + path + " Listening on UDP port: " + port);
        stopLatch.await();
    }
}
