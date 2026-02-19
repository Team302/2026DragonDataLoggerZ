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

import java.util.HashMap;
import java.util.Map;

import edu.wpi.first.math.geometry.Pose2d;
import pi.logger.structs.ChassisSpeeds;
import pi.logger.structs.SwerveModulePosition;
import pi.logger.structs.SwerveModuleState;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StructArraySubscriber;
import edu.wpi.first.networktables.StructSubscriber;
import edu.wpi.first.networktables.Topic;
import edu.wpi.first.networktables.TopicInfo;
import edu.wpi.first.util.struct.Struct;
import pi.logger.telemetry.TelemetryEvent;
import pi.logger.telemetry.TelemetryPayloadType;
import pi.logger.telemetry.TelemetryProcessor;
import pi.logger.telemetry.TelemetrySource;

public final class NetworkTablesLogger {
    
    private static volatile boolean running = true;
    private static final int SWERVE_MODULE_COUNT = 4;
    private static final long TOPIC_DUMP_INTERVAL_MS = 5000; // 5 seconds
    private static long lastTopicDumpMs = 0;
    
    // Subscribers
    private static StructSubscriber<Pose2d> poseSubscriber;
    private static StructSubscriber<ChassisSpeeds> chassisSpeedsSubscriber;
    private static StructArraySubscriber<SwerveModulePosition> modulePositionsSubscriber;
    private static StructArraySubscriber<SwerveModuleState> moduleStatesSubscriber;
    private static StructArraySubscriber<SwerveModuleState> moduleTargetsSubscriber;
    // DriveState table reference used for simple entries (e.g., doubles)
    private static NetworkTable driveStateTable;

    private NetworkTablesLogger() {}

    public static void start() {
        Thread t = new Thread(NetworkTablesLogger::run, "nt-logger");
        t.setDaemon(true);
        t.start();
    }

    public static void stop() {
        running = false;
    }

    private static void run() {
        try {
            // Subscribe to NetworkTables topics
            NetworkTableInstance inst = NtClient.get();
            
            // Subscribe to DriveState topics
            driveStateTable = inst.getTable("DriveState");
            poseSubscriber = driveStateTable.getStructTopic("Pose", Pose2d.struct).subscribe(new Pose2d());
            chassisSpeedsSubscriber = driveStateTable.getStructTopic("Speeds", ChassisSpeeds.struct).subscribe(new ChassisSpeeds());
            modulePositionsSubscriber = driveStateTable
                .getStructArrayTopic("ModulePositions", SwerveModulePosition.struct)
                .subscribe(createDefaultModulePositions());
            moduleStatesSubscriber = driveStateTable
                .getStructArrayTopic("ModuleStates", SwerveModuleState.struct)
                .subscribe(createDefaultModuleStates());
            moduleTargetsSubscriber = driveStateTable
                .getStructArrayTopic("ModuleTargets", SwerveModuleState.struct)
                .subscribe(createDefaultModuleStates());

            System.out.println("NetworkTables logger started");

            while (running) {
                dumpTopicsPeriodically(inst);
                // Log Pose2D
                logPose2D();
                // Log chassis speeds
                logChassisSpeeds();
                // Log module states
                logModuleStates();
                // Log odometry frequency
                logOdometryFrequency();
                // Log module positions
                logModulePositions();
                // Log module targets
                logModuleTargets();

                // Update rate: 50Hz
                Thread.sleep(20);
            }

        } catch (Exception e) {
            System.err.println("NetworkTables logger error");
            e.printStackTrace();
        } finally {
            closeSubs();
        }
    }

    private static void logPose2D() {
        try {
            Pose2d pose = poseSubscriber.get();
            
            if (pose != null) {
                
                publishStruct("DriveState/Pose", pose, Pose2d.struct);
            
            }
        } catch (Exception e) {
            System.err.println("Error logging Pose2D: " + e.getMessage());
        }
    }

    private static void logChassisSpeeds() {
        try {
            ChassisSpeeds speeds = chassisSpeedsSubscriber.get();
            if (speeds != null) {
                publishStruct("DriveState/ChassisSpeeds", speeds, ChassisSpeeds.struct);
            }
        } catch (Exception e) {
            System.err.println("Error logging ChassisSpeeds: " + e.getMessage());
        }
    }

    private static void logModulePositions() {
        try {
            SwerveModulePosition[] positions = modulePositionsSubscriber.get();
            if (positions != null && positions.length > 0) {
                publishStructArray("DriveState/ModulePositions", positions, SwerveModulePosition.struct);
            }
        } catch (Exception e) {
            System.err.println("Error logging ModulePositions: " + e.getMessage());
        }
    }

    private static void logOdometryFrequency() {
        try {
            if (driveStateTable == null) return;
            double freq = driveStateTable.getEntry("OdometryFrequency").getDouble(Double.NaN);
            if (!Double.isNaN(freq)) {
                publishScalar(TelemetryPayloadType.DOUBLE, "DriveState/OdometryFrequency", freq);
            }
        } catch (Exception e) {
            System.err.println("Error logging OdometryFrequency: " + e.getMessage());
        }
    }

    private static void logModuleStates() {
        try {
            SwerveModuleState[] states = moduleStatesSubscriber.get();
            if (states != null && states.length > 0) {
                publishStructArray("DriveState/ModuleStates", states, SwerveModuleState.struct);
            }
        } catch (Exception e) {
            System.err.println("Error logging ModuleStates: " + e.getMessage());
        }
    }

    private static void logModuleTargets() {
        try {
            SwerveModuleState[] targets = moduleTargetsSubscriber.get();
            if (targets != null && targets.length > 0) {
                publishStructArray("DriveState/ModuleTargets", targets, SwerveModuleState.struct);
            }
        } catch (Exception e) {
            System.err.println("Error logging ModuleTargets: " + e.getMessage());
        }
    }

    private static SwerveModulePosition[] createDefaultModulePositions() {
        SwerveModulePosition[] defaults = new SwerveModulePosition[SWERVE_MODULE_COUNT];
        for (int i = 0; i < SWERVE_MODULE_COUNT; i++) {
            defaults[i] = new SwerveModulePosition();
        }
        return defaults;
    }

    private static SwerveModuleState[] createDefaultModuleStates() {
        SwerveModuleState[] defaults = new SwerveModuleState[SWERVE_MODULE_COUNT];
        for (int i = 0; i < SWERVE_MODULE_COUNT; i++) {
            defaults[i] = new SwerveModuleState();
        }
        return defaults;
    }

    private static void closeSubs() {
        if (poseSubscriber != null) {
            poseSubscriber.close();
        }
        if (chassisSpeedsSubscriber != null) {
            chassisSpeedsSubscriber.close();
        }
        if (modulePositionsSubscriber != null) {
            modulePositionsSubscriber.close();
        }
        if (moduleStatesSubscriber != null) {
            moduleStatesSubscriber.close();
        }
        if (moduleTargetsSubscriber != null) {
            moduleTargetsSubscriber.close();
        }
    }

    private static void dumpAllNetworkTableKeys(NetworkTableInstance inst) {
        try {
            Topic[] topics = inst.getTopics();
            TopicInfo[] infos = inst.getTopicInfo();
            Map<String, TopicInfo> infoByName = new HashMap<>();
            for (TopicInfo info : infos) {
                infoByName.put(info.name, info);
            }
            System.out.println("---- NetworkTables Topics (" + topics.length + ") ----");
            for (Topic topic : topics) {
                TopicInfo info = infoByName.get(topic.getName());
        String typeLabel = (info != null && info.typeStr != null && !info.typeStr.isBlank())
            ? info.typeStr
            : topic.getType().toString();
                System.out.printf("%s (type=%s)%n", topic.getName(), typeLabel);
            }
            System.out.println("---- End NetworkTables Topics ----");
        } catch (Exception e) {
            System.err.println("Failed to enumerate NetworkTables topics: " + e.getMessage());
        }
    }

    private static void dumpTopicsPeriodically(NetworkTableInstance inst) {
        long now = System.currentTimeMillis();
        if (now - lastTopicDumpMs >= TOPIC_DUMP_INTERVAL_MS) {
            dumpAllNetworkTableKeys(inst);
            lastTopicDumpMs = now;
        }
    }

    private static void publishStruct(String channel, Object value, Struct<?> struct) {
        if (value == null || struct == null) {
            return;
        }
        TelemetryEvent event = new TelemetryEvent(
            System.nanoTime(),
            TelemetrySource.NETWORK_TABLES,
            TelemetryPayloadType.STRUCT,
            channel,
            value,
            struct
        );
        TelemetryProcessor.publish(event);
    }

    private static void publishStructArray(String channel, Object[] values, Struct<?> struct) {
        if (values == null || values.length == 0) {
            return;
        }
        TelemetryEvent event = new TelemetryEvent(
            System.nanoTime(),
            TelemetrySource.NETWORK_TABLES,
            TelemetryPayloadType.STRUCT_ARRAY,
            channel,
            values,
            struct
        );
        TelemetryProcessor.publish(event);
    }

    private static void publishScalar(TelemetryPayloadType type, String channel, Object value) {
        if (value == null) {
            return;
        }
        TelemetryEvent event = new TelemetryEvent(
            System.nanoTime(),
            TelemetrySource.NETWORK_TABLES,
            type,
            channel,
            value,
            null
        );
        TelemetryProcessor.publish(event);
    }
}
