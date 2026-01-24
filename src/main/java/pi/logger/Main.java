package pi.logger;

import edu.wpi.first.util.datalog.DoubleLogEntry;
import edu.wpi.first.util.datalog.DataLog;
import edu.wpi.first.util.datalog.WPILOGWriter;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Pi logger starting");


    }
}
public class PiLogger {
    private DataLog log;
    private DoubleLogEntry visionLatency;

    public PiLogger() {
        // Explicitly set the path to your mounted USB folder
        // The file will be saved as a .wpilog compatible with AdvantageScope
        log = new DataLog("/mnt/usb_logs", "PiVisionLog.wpilog");
        
        // Create an entry for specific data
        visionLatency = new DoubleLogEntry(log, "/vision/latency_ms");
    }

    public void logData(double latency) {
        visionLatency.append(latency);
    }
    public class PiLogger {
    private DataLog log;
    private DoubleLogEntry visionLatency;

    public PiLogger() {
        // Explicitly set the path to your mounted USB folder
        // The file will be saved as a .wpilog compatible with AdvantageScope
        log = new DataLog("/mnt/usb_logs", "PiVisionLog.wpilog");
        
        // Create an entry for specific data
        visionLatency = new DoubleLogEntry(log, "/vision/latency_ms");
    }
    
    public void logData(double latency) {
        visionLatency.append(latency);
    }
    
}