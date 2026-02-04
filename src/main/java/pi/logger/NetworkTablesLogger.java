package pi.logger;

import edu.wpi.first.math.geometry.Pose2d;
import pi.logger.structs.ChassisSpeeds;
import pi.logger.structs.SwerveModulePosition;
import pi.logger.structs.SwerveModuleState;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StructSubscriber;

public final class NetworkTablesLogger {
    
    private static volatile boolean running = true;
    
    // Subscribers
    private static StructSubscriber<Pose2d> poseSubscriber;
    private static StructSubscriber<ChassisSpeeds> chassisSpeedsSubscriber;
    private static StructSubscriber<SwerveModulePosition> modulePositionsSubscriber;
    private static StructSubscriber<SwerveModuleState> moduleStatesSubscriber;
    private static StructSubscriber<SwerveModuleState> moduleTargetsSubscriber;
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
            modulePositionsSubscriber = driveStateTable.getStructTopic("ModulePositions", SwerveModulePosition.struct).subscribe(new SwerveModulePosition());
            moduleStatesSubscriber = driveStateTable.getStructTopic("ModuleStates", SwerveModuleState.struct).subscribe(new SwerveModuleState());
            moduleTargetsSubscriber = driveStateTable.getStructTopic("ModuleTargets", SwerveModuleState.struct).subscribe(new SwerveModuleState());

            System.out.println("NetworkTables logger started");

            while (running) {
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
                
                // Log the entire Pose2d struct using USBFileLogger
                USBFileLogger.logStruct("DriveState/Pose", pose);
            
            }
        } catch (Exception e) {
            System.err.println("Error logging Pose2D: " + e.getMessage());
        }
    }

    private static void logChassisSpeeds() {
        try {
            ChassisSpeeds speeds = chassisSpeedsSubscriber.get();
            if (speeds != null) {
                USBFileLogger.logStruct("DriveState/ChassisSpeeds", speeds);
            }
        } catch (Exception e) {
            System.err.println("Error logging ChassisSpeeds: " + e.getMessage());
        }
    }

    private static void logModulePositions() {
        try {
            SwerveModulePosition pos = modulePositionsSubscriber.get();
            if (pos != null) {
                USBFileLogger.logStruct("DriveState/ModulePositions", pos);
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
                USBFileLogger.logDouble("DriveState/OdometryFrequency", freq);
            }
        } catch (Exception e) {
            System.err.println("Error logging OdometryFrequency: " + e.getMessage());
        }
    }

    private static void logModuleStates() {
        try {
            SwerveModuleState state = moduleStatesSubscriber.get();
            if (state != null) {
                USBFileLogger.logStruct("DriveState/ModuleStates", state);
            }
        } catch (Exception e) {
            System.err.println("Error logging ModuleStates: " + e.getMessage());
        }
    }

    private static void logModuleTargets() {
        try {
            SwerveModuleState target = moduleTargetsSubscriber.get();
            if (target != null) {
                USBFileLogger.logStruct("DriveState/ModuleTargets", target);
            }
        } catch (Exception e) {
            System.err.println("Error logging ModuleTargets: " + e.getMessage());
        }
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



}
