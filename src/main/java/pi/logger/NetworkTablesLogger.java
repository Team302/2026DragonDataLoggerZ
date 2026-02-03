package pi.logger;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StructSubscriber;

public final class NetworkTablesLogger {
    
    private static volatile boolean running = true;
    
    // Subscribers
    private static StructSubscriber<Pose2d> poseSubscriber;

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
            
            // Subscribe to Pose2D
            NetworkTable driveStateTable = inst.getTable("DriveState");
            poseSubscriber = driveStateTable.getStructTopic("Pose", Pose2d.struct).subscribe(new Pose2d());

            System.out.println("NetworkTables logger started");

            while (running) {
                // Log Pose2D
                logPose2D();

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
                
                USBFileLogger.flush();
            
            }
        } catch (Exception e) {
            System.err.println("Error logging Pose2D: " + e.getMessage());
        }
    }

    private static void closeSubs() {
        if (poseSubscriber != null) {
            poseSubscriber.close();
        }
    }
}
