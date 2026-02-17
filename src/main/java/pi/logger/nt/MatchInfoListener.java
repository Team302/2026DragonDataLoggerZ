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

import edu.wpi.first.networktables.*;
import edu.wpi.first.networktables.NetworkTableEvent.Kind;

import java.util.EnumSet;

public final class MatchInfoListener {

    private static volatile int matchNumber = -1;
    private static volatile boolean enabled = false;

    private MatchInfoListener() {}

    public static void start() {
        NetworkTableInstance inst = NtClient.get();
        NetworkTable fms = inst.getTable("FMSInfo");
        NetworkTable ds  = inst.getTable("DriverStation");
        inst.addListener(
                fms.getEntry("MatchNumber"),
                EnumSet.of(Kind.kValueAll),
                event -> {
                    matchNumber = (int) event.valueData.value.getInteger();
                    System.out.println("Match number = " + matchNumber);
                }
        );
        inst.addListener(
                ds.getEntry("Enabled"),
                EnumSet.of(Kind.kValueAll),
                event -> {
                    enabled = event.valueData.value.getBoolean();
                    System.out.println("Robot enabled = " + enabled);
                }
        );
    }

    public static int getMatchNumber() {
        return matchNumber;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}

