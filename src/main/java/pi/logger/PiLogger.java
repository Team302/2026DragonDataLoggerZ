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

import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.math.jni.WPIMathJNI;
import edu.wpi.first.util.WPIUtilJNI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

//import edu.wpi.first.util.datalog.DoubleLogEntry;
//import edu.wpi.first.util.datalog.DataLog;
//import edu.wpi.first.util.datalog.WPILOGWriter;

public class PiLogger {
    
    private static void extractAndLoadLibrary(String libName) throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch");
        String platform;
        String libPrefix;
        String libExt;
        
        if (osName.contains("win")) {
            platform = "windows/x86-64";
            libPrefix = "";
            libExt = ".dll";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            platform = "linux/arm64";
            libPrefix = "lib";
            libExt = ".so";
        } else {
            platform = "linux/x86-64";
            libPrefix = "lib";
            libExt = ".so";
        }
        
        String resourcePath = "/" + platform + "/shared/" + libPrefix + libName + libExt;
        System.out.println("Attempting to load library from: " + resourcePath);
        
        try (InputStream in = PiLogger.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Library not found in JAR: " + resourcePath);
            }
            
            // Create temp file
            File tempDir = Files.createTempDirectory("wpilib_natives").toFile();
            tempDir.deleteOnExit();
            File tempLib = new File(tempDir, libPrefix + libName + libExt);
            tempLib.deleteOnExit();
            
            // Extract library
            try (FileOutputStream out = new FileOutputStream(tempLib)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            // Load library
            System.load(tempLib.getAbsolutePath());
            System.out.println("Successfully loaded: " + libName);
        }
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("Pi logger starting");
        
        // Disable automatic extraction - we load manually
        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPIMathJNI.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        
        // Manually extract and load libraries in dependency order
        // Base libraries first, then their JNI wrappers
        try {
            extractAndLoadLibrary("wpiutil");
            extractAndLoadLibrary("wpiutiljni");
            
            extractAndLoadLibrary("wpinet");
            
            extractAndLoadLibrary("wpimath");
            extractAndLoadLibrary("wpimathjni");
            
            extractAndLoadLibrary("ntcore");
            extractAndLoadLibrary("ntcorejni");
        } catch (IOException e) {
            System.err.println("Failed to load native libraries: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        new PiLogger().run();



    }

    public void run() {

        NtClient.start("localhost");

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
