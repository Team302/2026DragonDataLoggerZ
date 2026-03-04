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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class MatchInfoListener {

    private static final Logger LOG = LoggerFactory.getLogger(MatchInfoListener.class);
    private static volatile boolean enabled         = false;
    private static volatile boolean autonomous      = false;
    private static volatile boolean operatorControl = false;
    private static volatile boolean testMode        = false;
    private static volatile boolean eStop           = false;
    private static volatile boolean fmsAttached     = false;
    private static volatile boolean dsAttached      = false;
    private static volatile String  robotState      = "Disabled";

    // FMSInfo
    private static volatile int     matchNumber         = -1;
    private static volatile String  fmsType             = "";
    private static volatile String  gameSpecificMessage = "";
    private static volatile String  eventName           = "";
    private static volatile int     replayNumber        = 0;
    private static volatile int     matchType           = 0;
    private static volatile boolean isRedAlliance       = false;
    private static volatile int     stationNumber       = 0;
    private static volatile int     fmsControlData      = 0;

    private MatchInfoListener() {}

    public static void start() {
        NetworkTableInstance inst = NtClient.get();
        NetworkTable fms = inst.getTable("FMSInfo");

        // ---- Initial reads ----
        matchNumber         = (int) fms.getEntry("MatchNumber").getNumber(-1).intValue();
        fmsType             = fms.getEntry(".type").getString("");
        gameSpecificMessage = fms.getEntry("GameSpecificMessage").getString("");
        eventName           = fms.getEntry("EventName").getString("");
        replayNumber        = (int) fms.getEntry("ReplayNumber").getNumber(0).intValue();
        matchType           = (int) fms.getEntry("MatchType").getNumber(0).intValue();
        isRedAlliance       = fms.getEntry("IsRedAlliance").getBoolean(false);
        stationNumber       = (int) fms.getEntry("StationNumber").getNumber(0).intValue();
        fmsControlData      = (int) fms.getEntry("FMSControlData").getNumber(0).intValue();

        // Parse bitmask into component fields on startup (no prior value to compare — treat old as 0)
        applyFmsControlData(0, fmsControlData);

        LOG.info("MatchInfoListener initial:"
                + " matchNumber=" + matchNumber
                + " fmsControlData=" + String.format("0x%08X", fmsControlData)
                + " enabled=" + enabled
                + " robotState=" + robotState
                + " eventName=\"" + eventName + "\""
                + " matchType=" + matchType
                + " isRedAlliance=" + isRedAlliance
                + " station=" + stationNumber);
        // Don't call updateRobotState() again — applyFmsControlData already called it.

        // ---- FMSInfo listeners ----
        inst.addListener(fms.getEntry("MatchNumber"), EnumSet.of(Kind.kValueAll), event -> {
            int v = (int) event.valueData.value.getInteger();
            if (v != matchNumber) { matchNumber = v; log("MatchNumber", v); }
        });
        inst.addListener(fms.getEntry(".type"), EnumSet.of(Kind.kValueAll), event -> {
            String v = event.valueData.value.getString();
            if (!v.equals(fmsType)) { fmsType = v; log("FMSInfo/.type", v); }
        });
        inst.addListener(fms.getEntry("GameSpecificMessage"), EnumSet.of(Kind.kValueAll), event -> {
            String v = event.valueData.value.getString();
            if (!v.equals(gameSpecificMessage)) { gameSpecificMessage = v; log("GameSpecificMessage", v); }
        });
        inst.addListener(fms.getEntry("EventName"), EnumSet.of(Kind.kValueAll), event -> {
            String v = event.valueData.value.getString();
            if (!v.equals(eventName)) { eventName = v; log("EventName", v); }
        });
        inst.addListener(fms.getEntry("ReplayNumber"), EnumSet.of(Kind.kValueAll), event -> {
            int v = (int) event.valueData.value.getInteger();
            if (v != replayNumber) { replayNumber = v; log("ReplayNumber", v); }
        });
        inst.addListener(fms.getEntry("MatchType"), EnumSet.of(Kind.kValueAll), event -> {
            int v = (int) event.valueData.value.getInteger();
            if (v != matchType) { matchType = v; log("MatchType", v); }
        });
        inst.addListener(fms.getEntry("IsRedAlliance"), EnumSet.of(Kind.kValueAll), event -> {
            boolean v = event.valueData.value.getBoolean();
            if (v != isRedAlliance) { isRedAlliance = v; log("IsRedAlliance", v); }
        });
        inst.addListener(fms.getEntry("StationNumber"), EnumSet.of(Kind.kValueAll), event -> {
            int v = (int) event.valueData.value.getInteger();
            if (v != stationNumber) { stationNumber = v; log("StationNumber", v); }
        });
        inst.addListener(fms.getEntry("FMSControlData"), EnumSet.of(Kind.kValueAll), event -> {
            int v = (int) event.valueData.value.getInteger();
            if (v != fmsControlData) {
                int prev = fmsControlData;
                fmsControlData = v;
                log("FMSControlData", String.format("0x%08X", v));
                applyFmsControlData(prev, v);
            }
        });

        // ---- Polling fallback (catches updates when NT doesn't emit events) ----
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "matchinfo-poller");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(() -> {
            try {
                poll("MatchNumber",          (int) fms.getEntry("MatchNumber").getNumber(-1).intValue());
                poll("GameSpecificMessage",  fms.getEntry("GameSpecificMessage").getString(""));
                poll("EventName",            fms.getEntry("EventName").getString(""));
                poll("ReplayNumber",         (int) fms.getEntry("ReplayNumber").getNumber(0).intValue());
                poll("MatchType",            (int) fms.getEntry("MatchType").getNumber(0).intValue());
                poll("IsRedAlliance",        fms.getEntry("IsRedAlliance").getBoolean(false));
                poll("StationNumber",        (int) fms.getEntry("StationNumber").getNumber(0).intValue());
                poll("FMSControlData",       (int) fms.getEntry("FMSControlData").getNumber(0).intValue());
            } catch (Exception ignored) {}
        }, 1, 500, TimeUnit.MILLISECONDS);


    }

    /** Called by the poller; updates the field and logs if the value changed. */
    private static synchronized void poll(String key, Object newVal) {
        switch (key) {
            case "MatchNumber":         { int v = (int) newVal;     if (v != matchNumber)         { matchNumber = v;         log(key, v); } break; }
            case "GameSpecificMessage": { String v = (String) newVal; if (!v.equals(gameSpecificMessage)) { gameSpecificMessage = v; log(key, v); } break; }
            case "EventName":           { String v = (String) newVal; if (!v.equals(eventName))    { eventName = v;           log(key, v); } break; }
            case "ReplayNumber":        { int v = (int) newVal;     if (v != replayNumber)         { replayNumber = v;        log(key, v); } break; }
            case "MatchType":           { int v = (int) newVal;     if (v != matchType)            { matchType = v;           log(key, v); } break; }
            case "IsRedAlliance":       { boolean v = (boolean) newVal; if (v != isRedAlliance)    { isRedAlliance = v;       log(key, v); } break; }
            case "StationNumber":       { int v = (int) newVal;     if (v != stationNumber)        { stationNumber = v;       log(key, v); } break; }
            case "FMSControlData":      { int v = (int) newVal;     if (v != fmsControlData)       { int prev = fmsControlData; fmsControlData = v; log("FMSControlData", String.format("0x%08X", v)); applyFmsControlData(prev, v); } break; }
            case "Enabled":             { boolean v = (boolean) newVal; enabled = v; break; }
            case "Autonomous":          { boolean v = (boolean) newVal; autonomous = v; break; }
            case "OperatorControl":     { boolean v = (boolean) newVal; operatorControl = v; break; }
            case "Test":                { boolean v = (boolean) newVal; testMode = v; break; }
            default: break;
        }
    }

    private static void log(String key, Object value) {
        LOG.debug("{} = {}", key, value);
    }

    /**
     * Parses the FMSControlData bitmask and updates the derived boolean fields.
     * Logs any bit that changed when {@code logChanges} is true.
     *
     * <p>HAL_ControlWord bit layout (from DriverStationTypes.h):
     * <pre>
     *   bit 0 (0x01) – enabled
     *   bit 1 (0x02) – autonomous
     *   bit 2 (0x04) – test
     *   bit 3 (0x08) – e-stop
     *   bit 4 (0x10) – FMS attached
     *   bit 5 (0x20) – DS attached
     *   (operatorControl = enabled &amp;&amp; !autonomous &amp;&amp; !test)
     * </pre>
     *
     * <p>Note: if FMSControlData is 0 (not yet published), the individual
     * DriverStation/Enabled, Autonomous, OperatorControl, Test keys are used
     * as the authoritative source instead.
     */
    private static synchronized void applyFmsControlData(int oldData, int newData) {
        // If FMSControlData is 0, it may simply not have been published yet.
        // In that case, don't override the values already set by the individual DS keys.
        if (newData == 0) return;

        boolean newEnabled     = (newData & 0x01) != 0;
        boolean newAuto        = (newData & 0x02) != 0;
        boolean newTest        = (newData & 0x04) != 0;
        boolean newEStop       = (newData & 0x08) != 0;
        boolean newFmsAttached = (newData & 0x10) != 0;
        boolean newDsAttached  = (newData & 0x20) != 0;
        boolean newOpControl   = newEnabled && !newAuto && !newTest;

        // Derive old values from oldData bitmask to avoid races with DS key listeners
        boolean oldEnabled     = (oldData & 0x01) != 0;
        boolean oldAuto        = (oldData & 0x02) != 0;
        boolean oldTest        = (oldData & 0x04) != 0;
        boolean oldEStop       = (oldData & 0x08) != 0;
        boolean oldFmsAttached = (oldData & 0x10) != 0;
        boolean oldDsAttached  = (oldData & 0x20) != 0;
        boolean oldOpControl   = oldEnabled && !oldAuto && !oldTest;

        // Log only when called from a live change (oldData != newData implies a real transition).
        // On startup (oldData == 0, newData == 0) we already returned above.
        // On startup with a real initial value, oldData == 0 so all bits appear "changed" —
        // suppress startup noise by only logging when oldData != 0.
        boolean logChanges = (oldData != 0);

        if (logChanges) {
            if (newEnabled     != oldEnabled)     log("Enabled",         newEnabled);
            if (newAuto        != oldAuto)         log("Autonomous",      newAuto);
            if (newOpControl   != oldOpControl)    log("OperatorControl", newOpControl);
            if (newTest        != oldTest)         log("Test",            newTest);
            if (newEStop       != oldEStop)        log("EStop",           newEStop);
            if (newFmsAttached != oldFmsAttached)  log("FMSAttached",     newFmsAttached);
            if (newDsAttached  != oldDsAttached)   log("DSAttached",      newDsAttached);
        }

        enabled         = newEnabled;
        autonomous      = newAuto;
        testMode        = newTest;
        eStop           = newEStop;
        fmsAttached     = newFmsAttached;
        dsAttached      = newDsAttached;
        operatorControl = newOpControl;

        updateRobotState();
    }

    private static void updateRobotState() {
        String prev = robotState;
        if (autonomous) robotState = "Autonomous";
        else if (operatorControl) robotState = "Teleoperated";
        else if (testMode) robotState = "Test";
        else robotState = "Disabled";
        if (!robotState.equals(prev)) {
            LOG.info("Robot state = {}", robotState);
        }
    }

    public static int     getMatchNumber()         { return matchNumber; }
    public static String  getFmsType()              { return fmsType; }
    public static String  getGameSpecificMessage()  { return gameSpecificMessage; }
    public static String  getEventName()            { return eventName; }
    public static int     getReplayNumber()         { return replayNumber; }
    public static int     getMatchType()            { return matchType; }
    public static boolean isRedAlliance()           { return isRedAlliance; }
    public static int     getStationNumber()        { return stationNumber; }
    public static int     getFmsControlData()       { return fmsControlData; }
    public static boolean isEnabled()               { return enabled; }
    public static boolean isEStop()                 { return eStop; }
    public static boolean isFmsAttached()           { return fmsAttached; }
    public static boolean isDsAttached()            { return dsAttached; }
    public static String  getRobotState()           { return robotState; }
}

