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

package pi.logger.telemetry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PiHealthMonitor {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PiHealthMonitor");
        t.setDaemon(true);
        return t;
    });

    private final Consumer<TelemetryEvent> logConsumer;

    /**
     * @param logConsumer A callback to accept the generated TelemetryEvent
     */
    public PiHealthMonitor(Consumer<TelemetryEvent> logConsumer) {
        this.logConsumer = logConsumer;
    }

    public void start(long updateIntervalSeconds) {
        scheduler.scheduleAtFixedRate(this::pollAndLog, 0, updateIntervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void pollAndLog() {
        try {
            double voltage = getCoreVoltage();
            long throttledFlags = getThrottledFlags();

            boolean underVoltage = (throttledFlags & 0x1L) != 0;
            // The Pi doesn't natively report "over-voltage", so we use a safe threshold guess (e.g., > 1.35V)
            boolean overVoltage = voltage > 1.35;

            String state = underVoltage ? "under" : (overVoltage ? "over" : "healthy");

            long timestamp = System.currentTimeMillis() * 1000;

            // Log the health state string ("under", "healthy", "over")
            TelemetryEvent stateEvent = new TelemetryEvent(
                timestamp,
                TelemetrySource.SYSTEM,
                TelemetryPayloadType.STRING,
                "Pi/Health/State",
                state,
                null
            );

            // Log the live voltage
            TelemetryEvent voltageEvent = new TelemetryEvent(
                timestamp,
                TelemetrySource.SYSTEM,
                TelemetryPayloadType.DOUBLE,
                "Pi/Health/Voltage",
                voltage,
                null
            );

            logConsumer.accept(stateEvent);
            logConsumer.accept(voltageEvent);

        } catch (Exception e) {
            System.err.println("PiHealthMonitor Error: " + e.getMessage());
        }
    }

    private double getCoreVoltage() throws Exception {
        String line = runVcgenCmd("measure_volts", "core");
        if (line != null && line.startsWith("volt=")) {
            return Double.parseDouble(line.substring(5, line.length() - 1));
        }
        return -1.0;
    }

    private long getThrottledFlags() throws Exception {
        String line = runVcgenCmd("get_throttled");
        if (line != null && line.startsWith("throttled=")) {
            return Long.decode(line.substring(10).trim());
        }
        return 0L;
    }

    private String runVcgenCmd(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "vcgencmd";
        System.arraycopy(args, 0, cmd, 1, args.length);

        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        if (!p.waitFor(1, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            return null;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            return br.readLine();
        }
    }
}