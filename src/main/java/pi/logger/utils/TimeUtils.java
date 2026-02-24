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

import edu.wpi.first.util.WPIUtilJNI;

/**
 * Provides timestamps in microseconds relative to when {@link #initialize()} was first called,
 * so that log files start near t=0 regardless of the underlying platform clock.
 *
 * <p>On desktop Linux/Windows, {@code WPIUtilJNI.now()} returns microseconds since the Unix epoch.
 * By subtracting the value captured at startup we get a monotonically increasing timestamp that
 * begins near zero, which AdvantageScope expects for its timeline display.
 */
public final class TimeUtils {

    /** Epoch offset captured once at {@link #initialize()} time. */
    private static volatile long epochOffsetUs = 0;

    private TimeUtils() {}

    /**
     * Captures the current time as the zero-point for all subsequent {@link #nowUs()} calls.
     * Must be called once before any logging begins (e.g., at the start of {@code PiLogger.main}).
     */
    public static void initialize() {
        epochOffsetUs = WPIUtilJNI.now();
    }

    /**
     * Returns a timestamp in microseconds relative to when {@link #initialize()} was called.
     * The first call after initialize() will return a value close to 0.
     *
     * @return microseconds since logger start
     */
    public static long nowUs() {
        return WPIUtilJNI.now() - epochOffsetUs;
    }
}
