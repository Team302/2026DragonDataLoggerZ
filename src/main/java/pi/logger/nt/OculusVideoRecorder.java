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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;

import edu.wpi.first.networktables.BooleanSubscriber;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringArraySubscriber;
import edu.wpi.first.networktables.StringSubscriber;
import pi.logger.config.LoggerConfig;
import pi.logger.utils.FfmpegUtils;
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

    private static final int MAX_FILE_DURATION_SEC =
            LoggerConfig.getInt("oculus.maxFileDurationSec", 300, 10, 3600);


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
        FfmpegUtils.initFfmpegPath();

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
                    writeStatus("STOP", activeOutputPath, activeUrl, activeDescription, activeMode);
                    FfmpegUtils.stopFfmpeg(ffmpegProcess, activeUrl, activeOutputPath, activeDescription, activeMode);
                    ffmpegProcess = null;
                    activeUrl     = null;
                    activeOutputPath = null;
                    activeDescription = null;
                    activeMode = null;
                }

                if (shouldRecord && (ffmpegProcess == null || !ffmpegProcess.isAlive())) {
                    String outputPath = FfmpegUtils.buildOutputPath("oculus", description, mode);
                    ffmpegProcess = FfmpegUtils.startFfmpeg(streamUrl, outputPath);
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
                writeStatus("STOP", activeOutputPath, activeUrl, activeDescription, activeMode);
                FfmpegUtils.stopFfmpeg(ffmpegProcess, activeUrl, activeOutputPath, activeDescription, activeMode);
            }
            connectedSub.close();
            descriptionSub.close();
            modeSub.close();
            streamsSub.close();
            LOG.info("stopped");
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

        File f = new File(FfmpegUtils.getVideoDirPath(), "oculus_status.log");
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
