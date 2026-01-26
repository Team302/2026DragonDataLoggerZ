/**
 * //====================================================================================================================================================
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
 * Based on https://docs.wpilib.org/en/stable/docs/software/networktables/client-side-program.html
 */

package pi.logger;

import org.opencv.core.Core;

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.math.jni.WPIMathJNI;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;

//import edu.wpi.first.util.datalog.DoubleLogEntry;
//import edu.wpi.first.util.datalog.DataLog;
//import edu.wpi.first.util.datalog.WPILOGWriter;

public class PiLogger {
    public static void main(String[] args) throws Exception {
        System.out.println("Pi logger starting");
        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        WPIMathJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerJNI.Helper.setExtractOnStaticLoad(false);
        CombinedRuntimeLoader.loadLibraries(PiLogger.class, "wpiutiljni", "wpimathjni", "ntcorejni",
            Core.NATIVE_LIBRARY_NAME, "cscorejni");
        new PiLogger().run();



    }

    public void run() {

        NtClient.start("10.0.3.2");

        MatchInfoListener.start();
        HealthPublisher.start();

        UdpReceiver.start();
        USBFileLogger.start();

        System.out.println("Pi logger running");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.out.println("interrupted");
                return;
            }
        }
    }
}

/*public class PiLogger {
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

            
    
}*/