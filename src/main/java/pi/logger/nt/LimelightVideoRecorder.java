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

import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import pi.logger.config.LoggerConfig;
import pi.logger.utils.FfmpegUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records the MJPEG stream from a Limelight camera to a file using ffmpeg.
 *
 * <p>Unlike the Oculus recorder which discovers its stream URL via
 * NetworkTables ({@code CameraPublisher}), the Limelight exposes a fixed
 * MJPEG endpoint at {@code http://<host>:5800}. This class polls that URL
 * for reachability and starts/stops ffmpeg accordingly.
 *
 * <p>Recording is gated on two conditions:
 * <ol>
 *   <li>The Limelight stream URL is reachable (HTTP connection succeeds).</li>
 *   <li>The robot is enabled ({@link MatchInfoListener#isEnabled()}).</li>
 * </ol>
 *
 * <p>Config keys (logger.properties):
 * <ul>
 *   <li>{@code limelight.enabled}            – true/false (default true)</li>
 *   <li>{@code limelight.streamUrl}          – full stream URL
 *       (default {@code http://limelight-front.local:5800})</li>
 *   <li>{@code limelight.maxFileDurationSec} – max seconds per output file (default 300)</li>
 * </ul>
 *
 * <p>Video/ffmpeg settings ({@code oculus.videoDir}, {@code oculus.ffmpegPath},
 * {@code oculus.ffmpegExtraArgs}) are shared with {@link OculusVideoRecorder}
 * via {@link FfmpegUtils}.
 */
public final class LimelightVideoRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(LimelightVideoRecorder.class);

    private static final boolean ENABLED =
            LoggerConfig.getBoolean("limelight.enabled", true);

    private static final String STREAM_URL =
            LoggerConfig.getString("limelight.streamUrl", "http://limelight-front.local:5800");

    private static final int MAX_FILE_DURATION_SEC =
            LoggerConfig.getInt("limelight.maxFileDurationSec", 300, 10, 3600);

    /** Timeout (ms) for the HTTP reachability check. */
    private static final int CONNECT_TIMEOUT_MS = 2000;

    /** How often (ms) to re-check when the stream is not reachable. */
    private static final long POLL_INTERVAL_MS = 500;

    private static volatile boolean running = true;
    private static Thread recorderThread;

    private LimelightVideoRecorder() {}

    public static void start() {
        if (!ENABLED) {
            LOG.info("Limelight recorder disabled by config");
            return;
        }
        recorderThread = new Thread(LimelightVideoRecorder::run, "limelight-video-recorder");
        recorderThread.setDaemon(true);
        recorderThread.start();
    }

    public static void stop() {
        running = false;
        Thread t = recorderThread;
        if (t != null) {
            t.interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // Main loop
    // -------------------------------------------------------------------------

    private static void run() {
        FfmpegUtils.initFfmpegPath();

        LOG.info("watching Limelight stream at {}", STREAM_URL);
        writeStatus("WATCHING", null);

        Process ffmpegProcess = null;
        String activeOutputPath = null;
        long fileStartMs = 0;
        long lastUnreachableLogMs = 0;

        try {
            while (running) {
                boolean reachable = isStreamReachable();
                boolean robotActive = MatchInfoListener.isEnabled();
                boolean shouldRecord = robotActive && reachable;
                boolean durationExpired = ffmpegProcess != null
                        && (System.currentTimeMillis() - fileStartMs) >= (long) MAX_FILE_DURATION_SEC * 1000;

                // Stop current recording if conditions no longer met or file duration exceeded
                if (ffmpegProcess != null && (!shouldRecord || durationExpired)) {
                    writeStatus("STOP", activeOutputPath);
                    FfmpegUtils.stopFfmpeg(ffmpegProcess, STREAM_URL, activeOutputPath, "limelight", "mjpeg");
                    ffmpegProcess = null;
                    activeOutputPath = null;
                }

                // Start a new recording when conditions are met
                if (shouldRecord && (ffmpegProcess == null || !ffmpegProcess.isAlive())) {
                    String outputPath = FfmpegUtils.buildOutputPath("limelight", "limelight-front", "mjpeg");
                    ffmpegProcess = FfmpegUtils.startFfmpeg(STREAM_URL, outputPath);
                    if (ffmpegProcess != null) {
                        activeOutputPath = outputPath;
                        fileStartMs = System.currentTimeMillis();
                        LOG.info("recording started  url={}  output={}", STREAM_URL, outputPath);
                        writeStatus("START", outputPath);
                    }
                }

                // Periodically log when the stream is unreachable and the robot is active
                if (robotActive && !reachable && ffmpegProcess == null) {
                    long now = System.currentTimeMillis();
                    if (now - lastUnreachableLogMs >= 30_000) {
                        writeStatus("UNREACHABLE", null);
                        lastUnreachableLogMs = now;
                    }
                }

                Thread.sleep(POLL_INTERVAL_MS);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            if (ffmpegProcess != null) {
                writeStatus("STOP", activeOutputPath);
                FfmpegUtils.stopFfmpeg(ffmpegProcess, STREAM_URL, activeOutputPath, "limelight", "mjpeg");
            }
            LOG.info("stopped");
        }
    }

    // -------------------------------------------------------------------------
    // Connectivity check
    // -------------------------------------------------------------------------

    /**
     * Attempts a lightweight HTTP connection to the Limelight stream URL to
     * determine if the camera is reachable. Uses a short connect timeout so
     * the recorder loop is not blocked for long.
     */
    private static boolean isStreamReachable() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(STREAM_URL).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(CONNECT_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.connect();
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Status logging
    // -------------------------------------------------------------------------

    /**
     * Write a status line to a rolling status log file in the video directory.
     */
    private static synchronized void writeStatus(String action, String outputPath) {
        String ts = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        String line = String.format("%s %s url=%s output=%s",
                ts, action, STREAM_URL,
                outputPath == null ? "" : outputPath);

        File f = new File(FfmpegUtils.getVideoDirPath(), "limelight_status.log");
        try (FileWriter fw = new FileWriter(f, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.println(line);
            pw.flush();
        } catch (IOException e) {
            LOG.error("failed to write status log: {}", e.getMessage());
        }

        LOG.info("{}", line);
    }
}
