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
    private static final AtomicLong processedCount = new AtomicLong();
    private static final AtomicLong droppedCount = new AtomicLong();
    private static final AtomicLong errorCount = new AtomicLong();

    private static Thread workerThread;

    private TelemetryProcessor() {}

    public static void registerStage(TelemetryStage stage) {
        stages.add(stage);
    }

    public static long getProcessedCount() {
        return processedCount.get();
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
            droppedCount.incrementAndGet();
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
                processedCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
