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
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;

import edu.wpi.first.networktables.BooleanSubscriber;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringArraySubscriber;
import edu.wpi.first.networktables.StringSubscriber;
import pi.logger.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches the {@code CameraPublisher/Passthrough} NetworkTables entry published
 * by the Oculus and, whenever a valid stream URL is available and the camera is
 * connected, records the video stream to a file using ffmpeg.
 *
 * <p>NT entries consumed:
 * <ul>
 *   <li>{@code /CameraPublisher/Passthrough/connected}   – boolean
 *   <li>{@code /CameraPublisher/Passthrough/description} – string
 *   <li>{@code /CameraPublisher/Passthrough/mode}        – string
 *   <li>{@code /CameraPublisher/Passthrough/streams}     – string[]  (URL list)
 * </ul>
 *
 * <p>Config keys (logger.properties):
 * <ul>
 *   <li>{@code oculus.enabled}            – true/false (default true)
 *   <li>{@code oculus.videoDir}           – output directory (default /mnt/usb_logs/video)
 *   <li>{@code oculus.maxFileDurationSec} – max seconds per output file (default 300)
 *   <li>{@code oculus.ffmpegPath}         – path to ffmpeg binary (default ffmpeg)
 * </ul>
 */
public final class OculusVideoRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(OculusVideoRecorder.class);

    private static final String NT_TABLE      = "CameraPublisher/Passthrough";
    private static final String KEY_CONNECTED = "connected";
    private static final String KEY_DESC      = "description";
    private static final String KEY_MODE      = "mode";
    private static final String KEY_STREAMS   = "streams";

    // Preferred URL scheme order: mjpg/mjpeg over http is easiest to record with ffmpeg.
    // WPILib CameraServer uses "mjpg:" (e.g. "mjpg:http://10.x.x.x:5801/video").
    private static final String[] PREFERRED_PREFIXES = { "mjpg:", "mjpeg:", "http://", "rtsp://" };

    private static final boolean ENABLED =
            LoggerConfig.getBoolean("oculus.enabled", true);
    private static final String VIDEO_DIR_PATH =
            LoggerConfig.getString("oculus.videoDir", "/mnt/usb_logs/video");
    private static final int MAX_FILE_DURATION_SEC =
            LoggerConfig.getInt("oculus.maxFileDurationSec", 300, 10, 3600);
    private static final String FFMPEG_PATH =
            LoggerConfig.getString("oculus.ffmpegPath", "ffmpeg");
    private static final String FFMPEG_EXTRA_ARGS =
        LoggerConfig.getString("oculus.ffmpegExtraArgs", "");

    private static volatile boolean running = true;
    private static Thread recorderThread;

    private OculusVideoRecorder() {}

    public static void start() {
        if (!ENABLED) {
            LOG.info("disabled by config");
            return;
        }
        recorderThread = new Thread(OculusVideoRecorder::run, "oculus-video-recorder");
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
        try {
            Files.createDirectories(new File(VIDEO_DIR_PATH).toPath());
        } catch (IOException e) {
            LOG.error("cannot create video dir {}: {}", VIDEO_DIR_PATH, e.getMessage());
            return;
        }

        NetworkTableInstance inst = NtClient.get();
        NetworkTable table = inst.getTable(NT_TABLE);

        BooleanSubscriber   connectedSub    = table.getBooleanTopic(KEY_CONNECTED).subscribe(false);
        StringSubscriber    descriptionSub  = table.getStringTopic(KEY_DESC).subscribe("");
        StringSubscriber    modeSub         = table.getStringTopic(KEY_MODE).subscribe("");
        StringArraySubscriber streamsSub    = table.getStringArrayTopic(KEY_STREAMS).subscribe(new String[0]);

        LOG.info("watching CameraPublisher/Passthrough");
        // Ensure the status log exists and record that we're watching even if
        // no camera is currently published. This gives operators a visible
        // indication that the recorder process started.
        writeStatus("WATCHING", null, null, null, null);

        Process ffmpegProcess = null;
        String  activeUrl     = null;
        String  activeOutputPath = null;
        String  activeDescription = null;
        String  activeMode = null;
        long    fileStartMs   = 0;
        long    lastNoStreamLogMs = 0;

        try {
            while (running) {
                boolean connected  = connectedSub.get();
                String[] streams   = streamsSub.get();
                String   streamUrl = pickStreamUrl(streams);
                String description = descriptionSub.get();
                String mode        = modeSub.get();
                boolean robotActive = MatchInfoListener.isEnabled();
                boolean shouldRecord = robotActive && connected && streamUrl != null;
                boolean urlChanged   = !java.util.Objects.equals(streamUrl, activeUrl);
                boolean durationExpired = ffmpegProcess != null
                        && (System.currentTimeMillis() - fileStartMs) >= (long) MAX_FILE_DURATION_SEC * 1000;

                if (ffmpegProcess != null && (!shouldRecord || urlChanged || durationExpired)) {
                    // Stop current recording
                    stopFfmpeg(ffmpegProcess, activeUrl, activeOutputPath, activeDescription, activeMode);
                    ffmpegProcess = null;
                    activeUrl     = null;
                    activeOutputPath = null;
                    activeDescription = null;
                    activeMode = null;
                }

                if (shouldRecord && (ffmpegProcess == null || !ffmpegProcess.isAlive())) {
                    String outputPath = buildOutputPath(description, mode);
                    ffmpegProcess = startFfmpeg(streamUrl, outputPath);
                    if (ffmpegProcess != null) {
                        activeUrl   = streamUrl;
                        fileStartMs = System.currentTimeMillis();
                        LOG.info("recording started  url={}  desc={}  mode={}  output={}",
                                streamUrl, description, mode, outputPath);
                        // Log START so oculus_status.log contains both START and STOP
                        writeStatus("START", outputPath, streamUrl, description, mode);
                    }
                } 

                // If robot is active but no stream is available, periodically
                // write a NO_STREAM status 
                if (robotActive && streamUrl == null && activeUrl == null) {
                    long now = System.currentTimeMillis();
                    if (now - lastNoStreamLogMs >= 30_000) {
                        writeStatus("NO_STREAM", null, null, description, mode);
                        lastNoStreamLogMs = now;
                    }
                }

                Thread.sleep(500);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            if (ffmpegProcess != null) {
                stopFfmpeg(ffmpegProcess, activeUrl, activeOutputPath, activeDescription, activeMode);
            }
            connectedSub.close();
            descriptionSub.close();
            modeSub.close();
            streamsSub.close();
            LOG.info("stopped");
        }
    }

    // -------------------------------------------------------------------------
    // ffmpeg helpers
    // -------------------------------------------------------------------------

    /**
     * Launches ffmpeg to copy the stream into a Matroska container.
     * Using {@code -c copy} avoids transcoding on the Pi, keeping CPU load minimal.
     * A 2-second input timeout ({@code -timeout}) prevents indefinite hangs.
     */
    private static Process startFfmpeg(String streamUrl, String outputPath) {
        // Strip the "mjpeg:" scheme prefix that WPILib prepends; ffmpeg wants the raw URL
        String ffmpegUrl = stripMjpegScheme(streamUrl);

        java.util.List<String> cmdList = new java.util.ArrayList<>();
        cmdList.add(FFMPEG_PATH);
        cmdList.add("-loglevel");
        cmdList.add("warning");

        // Insert any extra ffmpeg args from config (allows changing behavior
        // without recompiling). The args string supports quoted tokens.
        for (String a : parseArgs(FFMPEG_EXTRA_ARGS)) {
            if (!a.isBlank()) cmdList.add(a);
        }

        // Add transport option only for RTSP URLs; some ffmpeg builds reject
        // -rtsp_transport when it's not applicable. If you need a different
        // transport, set it in oculus.ffmpegExtraArgs (e.g. -rtsp_transport tcp).
        String lowerUrl = ffmpegUrl == null ? "" : ffmpegUrl.toLowerCase();
        if (lowerUrl.startsWith("rtsp://")) {
            cmdList.add("-rtsp_transport");
            cmdList.add("tcp");
        }

        // NOTE: avoid passing a generic -timeout option here: not all builds
        // support it for all protocols. Let ffmpeg use defaults or encode
        // protocol-specific timeouts in the URL if necessary.

        cmdList.add("-i");
        cmdList.add(ffmpegUrl);
        cmdList.add("-c");
        cmdList.add("copy");
        cmdList.add("-y");
        cmdList.add(outputPath);

        String[] cmd = cmdList.toArray(new String[0]);

        LOG.debug("launching ffmpeg: {}", Arrays.toString(cmd));
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // merge stderr into stdout

            // Create a per-run ffmpeg log file in the video directory to aid debugging when
            // ffmpeg is launched from Java. Use a timestamp to avoid collisions.
            String logName = "ffmpeg_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now()) + ".log";
            File logFile = new File(VIDEO_DIR_PATH, logName);
            pb.redirectOutput(ProcessBuilder.Redirect.to(logFile));

            Process p = pb.start();
            LOG.debug("ffmpeg log={}", logFile.getAbsolutePath());
            return p;
        } catch (IOException e) {
            LOG.error("failed to start ffmpeg: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Stop ffmpeg and write a STOP status entry. Accepts the active output
     * path and metadata so we can log what was stopped.
     */
    private static void stopFfmpeg(Process process, String url, String outputPath, String description, String mode) {
        if (process == null) return;
        LOG.info("stopping ffmpeg (url={})", url);

        try {
            // Try a graceful shutdown by sending 'q' to ffmpeg's stdin. ffmpeg
            // responds to 'q' by finishing writes and exiting normally.
            try {
                java.io.OutputStream os = process.getOutputStream();
                if (os != null) {
                    os.write('q');
                    os.flush();
                    try { os.close(); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                LOG.error("failed to send quit to ffmpeg: {}", e.getMessage());
            }

            boolean exited = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!exited) {
                process.destroy();
                boolean exited2 = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                if (!exited2) {
                    process.destroyForcibly();
                    LOG.error("ffmpeg did not exit cleanly – force-killed");
                }
            }

            // Write STOP status so operators can see files being finalized
            writeStatus("STOP", outputPath, url, description, mode);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            try {
                process.destroyForcibly();
            } catch (Exception ignored) {}
        }
    }

    // -------------------------------------------------------------------------
    // URL / path utilities
    // -------------------------------------------------------------------------

    /**
     * From the array of stream URLs published by CameraServer, pick the most
     * suitable one for ffmpeg in preference order: mjpeg/http before rtsp.
     * Returns {@code null} if the array is empty or no usable URL is found.
     */
    private static String pickStreamUrl(String[] streams) {
        if (streams == null || streams.length == 0) return null;
        for (String prefix : PREFERRED_PREFIXES) {
            for (String url : streams) {
                if (url != null && url.toLowerCase().startsWith(prefix)) {
                    return url;
                }
            }
        }
        // Fall back to the first non-blank entry
        for (String url : streams) {
            if (url != null && !url.isBlank()) return url;
        }
        return null;
    }

    /**
     * WPILib CameraServer prefixes MJPEG URLs with {@code "mjpg:"} (e.g. {@code "mjpg:http://10.x.x.x:5801/video"}).
     * Strip that prefix so ffmpeg receives a plain {@code "http://"} URL.
     * Also handles the less-common {@code "mjpeg:"} variant.
     */
    private static String stripMjpegScheme(String url) {
        if (url == null) return null;
        String lower = url.toLowerCase();
        if (lower.startsWith("mjpg:")) {
            return url.substring("mjpg:".length());
        }
        if (lower.startsWith("mjpeg:")) {
            return url.substring("mjpeg:".length());
        }
        return url;
    }

    private static String buildOutputPath(String description, String mode) {
    // Include milliseconds to avoid collisions when multiple files start within the same second
    String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
        .withZone(ZoneId.systemDefault())
        .format(Instant.now());
        // Sanitise description/mode for use in filenames
        String safeDesc = sanitize(description);
        String safeMode = sanitize(mode);
        String name = "oculus_" + ts
                + (safeDesc.isEmpty() ? "" : "_" + safeDesc)
                + (safeMode.isEmpty() ? "" : "_" + safeMode)
                + ".mkv";
        return new File(VIDEO_DIR_PATH, name).getAbsolutePath();
    }

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "";
        return s.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_").replaceAll("_+", "_");
    }

    /**
     * Split a configuration string into tokens honoring single or double quotes.
     * Example: -rtsp_transport tcp -stimeout "5000000" -> [-rtsp_transport, tcp, 5000000]
     */
    private static List<String> parseArgs(String s) {
        List<String> parts = new ArrayList<>();
        if (s == null || s.isBlank()) return parts;
        Pattern p = Pattern.compile("\"([^\"]*)\"|'([^']*)'|([^\\s]+)");
        Matcher m = p.matcher(s);
        while (m.find()) {
            String token = m.group(1);
            if (token == null) token = m.group(2);
            if (token == null) token = m.group(3);
            if (token != null) parts.add(token);
        }
        return parts;
    }

    /**
     * Write a status line (START/STOP) to a rolling status log file in the
     * video directory so operators can see when recordings start/stop.
     */
    private static synchronized void writeStatus(String action, String outputPath, String url, String description, String mode) {
        String ts = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        String safeDesc = description == null ? "" : description;
        String safeMode = mode == null ? "" : mode;
        String line = String.format("%s %s output=%s url=%s desc=%s mode=%s", ts, action,
                outputPath == null ? "" : outputPath,
                url == null ? "" : url,
                safeDesc,
                safeMode);

        File f = new File(VIDEO_DIR_PATH, "oculus_status.log");
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
