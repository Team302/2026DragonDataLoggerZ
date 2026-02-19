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
package pi.logger.nt;

import edu.wpi.first.networktables.*;
import pi.logger.udp.UdpReceiver;

import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class HealthPublisher {

    private HealthPublisher() {}

    private static long heartbeat = 0;

    public static void start() {
        NetworkTable table =
                NtClient.get().getTable("pi");

        NetworkTableEntry connected =
                table.getEntry("connected");
        NetworkTableEntry queueDepth =
                table.getEntry("logQueueDepth");
        NetworkTableEntry diskFree =
                table.getEntry("diskFreeMB");
        NetworkTableEntry cpuLoad =
                table.getEntry("cpuLoad");
        NetworkTableEntry heartbeatEntry =
                table.getEntry("heartbeat");
        NetworkTableEntry messagesProcessedEntry =
                table.getEntry("messagesProcessed");

        connected.setBoolean(true);

        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-publisher");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(() -> {
            try {
                heartbeat++;
                heartbeatEntry.setInteger(heartbeat);

                queueDepth.setInteger(
                        UdpReceiver.getQueue().size());

                messagesProcessedEntry.setInteger(
                        UdpReceiver.getMessagesProcessed());

                diskFree.setDouble(getDiskFreeMB("/mnt/usb_logs"));

                cpuLoad.setDouble(getCpuLoad());

            } catch (Exception ignored) {}
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static double getDiskFreeMB(String path) throws Exception {
        FileStore store = Files.getFileStore(Path.of(path));
        return store.getUsableSpace() / 1e6;
    }

    private static double getCpuLoad() {
        var os =
                (com.sun.management.OperatingSystemMXBean)
                        ManagementFactory.getOperatingSystemMXBean();

        return os.getCpuLoad(); // 0.0–1.0
    }
}

