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
package pi.logger.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pi.logger.config.LoggerConfig;

public class FfmpegUtils {
    
    private static final String VIDEO_DIR_PATH =
        LoggerConfig.getString("oculus.videoDir", "/mnt/usb_logs/video");
    private static final String FFMPEG_PATH =
        LoggerConfig.getString("oculus.ffmpegPath", "ffmpeg");
    private static final String FFMPEG_EXTRA_ARGS =
        LoggerConfig.getString("oculus.ffmpegExtraArgs", "");
    private static final Logger LOG = LoggerFactory.getLogger(FfmpegUtils.class);
    // -------------------------------------------------------------------------
    // ffmpeg helpers
    // -------------------------------------------------------------------------

    public static void initFfmpegPath(){
        try {
            Files.createDirectories(new File(VIDEO_DIR_PATH).toPath());
        } catch (IOException e) {
            LOG.error("cannot create video dir {}: {}", VIDEO_DIR_PATH, e.getMessage());
            return;
        }
    }

    public static String getVideoDirPath() {
        return VIDEO_DIR_PATH;
    }

    /**
     * Launches ffmpeg to copy the stream into a Matroska container.
     * Using {@code -c copy} avoids transcoding on the Pi, keeping CPU load minimal.
     * A 2-second input timeout ({@code -timeout}) prevents indefinite hangs.
     */
    public static Process startFfmpeg(String streamUrl, String outputPath) {
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
    public static void stopFfmpeg(Process process, String url, String outputPath, String description, String mode) {
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


        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            try {
                process.destroyForcibly();
            } catch (Exception ignored) {}
        }
    }

    public static String buildOutputPath(String description, String mode) {
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

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "";
        return s.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_").replaceAll("_+", "_");
    }

}
