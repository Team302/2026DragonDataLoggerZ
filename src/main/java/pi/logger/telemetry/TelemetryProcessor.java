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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class TelemetryProcessor {

    private static final BlockingQueue<TelemetryEvent> inputQueue =
            new LinkedBlockingQueue<>(20_000);

    private static final List<TelemetryStage> stages = new CopyOnWriteArrayList<>();

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final AtomicLong receivedCount = new AtomicLong();
    private static final AtomicLong droppedCount = new AtomicLong();
    private static final AtomicLong errorCount = new AtomicLong();

    private static Thread workerThread;

    private TelemetryProcessor() {}

    public static void registerStage(TelemetryStage stage) {
        stages.add(stage);
    }

    public static long getReceivedCount() {
        return receivedCount.get();
    }

    public static long getDroppedCount() {
        return droppedCount.get();
    }

    public static long getErrorCount() {
        return errorCount.get();
    }

    public static int getPendingQueueSize() {
        return inputQueue.size();
    }

    public static void publish(TelemetryEvent event) {
        if (!inputQueue.offer(event)) {
            long drops = droppedCount.incrementAndGet();
            if (drops == 1 || drops % 1000 == 0) {
                System.err.println(
                        "[TelemetryProcessor] Dropped " + drops + " events; queue at capacity (" + inputQueue.size() + ")");
            }
        }
    }

    public static void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        workerThread = new Thread(TelemetryProcessor::run, "telemetry-processor");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public static void stop() {
        running.set(false);
        Thread t = workerThread;
        if (t != null) {
            t.interrupt();
        }
    }

    private static void run() {
        while (running.get()) {
            try {
                TelemetryEvent event = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (event == null) {
                    continue;
                }

                TelemetryContext context = new TelemetryContext(event);
                for (TelemetryStage stage : stages) {
                    try {
                        stage.apply(context);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Telemetry stage failed for channel " + event.channel());
                        e.printStackTrace();
                    }
                }
                receivedCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
