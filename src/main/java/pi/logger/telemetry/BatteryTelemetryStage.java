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
package pi.logger.telemetry;

import pi.logger.config.LoggerConfig;
import pi.logger.datalog.USBFileLogger;
import pi.logger.utils.TimeUtils;

/**
 * Telemetry stage that computes battery power metrics from RIO voltage and current events.
 *
 * <p>Every second it calculates instantaneous watts ({@code W = V × A}), accumulates watt-hour
 * usage, and detects battery replacements based on a configurable voltage-increment threshold.
 */
public final class BatteryTelemetryStage implements TelemetryStage {

    // Configurable NT event channel names (set in logger.properties)
    static final String DEFAULT_VOLTAGE_CHANNEL = "RIO/Voltage";
    static final String DEFAULT_CURRENT_CHANNEL = "RIO/Current";

    // Threshold above which a voltage increase signals a battery swap
    static final double DEFAULT_BATTERY_CHANGE_THRESHOLD_VOLTS = 0.5;

    static final double BATTERY_CHANGE_THRESHOLD_VOLTS = LoggerConfig.getDouble(
        "battery.changeThresholdVolts",
        DEFAULT_BATTERY_CHANGE_THRESHOLD_VOLTS,
        0.0
    );

    private static final String VOLTAGE_CHANNEL = LoggerConfig.getString(
        "battery.voltageChannel", DEFAULT_VOLTAGE_CHANNEL);
    private static final String CURRENT_CHANNEL = LoggerConfig.getString(
        "battery.currentChannel", DEFAULT_CURRENT_CHANNEL);

    // Log output channel names
    static final String WATTS_CHANNEL = "Battery/Watts";
    static final String WATT_HOURS_CHANNEL = "Battery/WattHours";
    static final String BATTERY_CHANGED_CHANNEL = "Battery/Changed";

    private static final double MILLIS_PER_HOUR = 3_600_000.0;

    // Internal state
    private double latestVoltage = Double.NaN;
    private double latestCurrent = Double.NaN;
    private double lastVoltageForChangeDetect = Double.NaN;
    private double totalWattHours = 0.0;
    private long lastComputeTimeMs = -1L;

    @Override
    public void apply(TelemetryContext context) throws Exception {
        updateReading(context.channel(), context.payload());
        maybeComputeAndLog(System.currentTimeMillis());
    }

    /**
     * Updates the cached voltage or current reading when a matching event arrives.
     */
    void updateReading(String channel, Object payload) {
        if (VOLTAGE_CHANNEL.equals(channel) && payload instanceof Number n) {
            latestVoltage = n.doubleValue();
        } else if (CURRENT_CHANNEL.equals(channel) && payload instanceof Number n) {
            latestCurrent = n.doubleValue();
        }
    }

    /**
     * Computes and logs watts/watt-hours if at least one second has elapsed since the last run.
     *
     * @param nowMs current wall-clock time in milliseconds
     */
    void maybeComputeAndLog(long nowMs) {
        if (lastComputeTimeMs < 0) {
            lastComputeTimeMs = nowMs;
            return;
        }
        long elapsedMs = nowMs - lastComputeTimeMs;
        if (elapsedMs < 1000) {
            return;
        }
        lastComputeTimeMs = nowMs;

        if (Double.isNaN(latestVoltage) || Double.isNaN(latestCurrent)) {
            return;
        }

        double watts = computeWatts(latestVoltage, latestCurrent);
        totalWattHours += watts * elapsedMs / MILLIS_PER_HOUR;
        long timestampUs = TimeUtils.nowUs();

        USBFileLogger.logDouble(WATTS_CHANNEL, watts, timestampUs);
        USBFileLogger.logDouble(WATT_HOURS_CHANNEL, totalWattHours, timestampUs);

        if (detectBatteryChange(latestVoltage)) {
            USBFileLogger.logBoolean(BATTERY_CHANGED_CHANNEL, true, timestampUs);
        }
    }

    /**
     * Returns {@code voltage * current}.
     */
    static double computeWatts(double voltage, double current) {
        return voltage * current;
    }

    /**
     * Returns {@code true} when {@code currentVoltage} exceeds the previous voltage by at least
     * {@link #BATTERY_CHANGE_THRESHOLD_VOLTS}, which indicates a battery was replaced.
     */
    boolean detectBatteryChange(double currentVoltage) {
        if (Double.isNaN(lastVoltageForChangeDetect)) {
            lastVoltageForChangeDetect = currentVoltage;
            return false;
        }
        boolean changed = (currentVoltage - lastVoltageForChangeDetect) >= BATTERY_CHANGE_THRESHOLD_VOLTS;
        lastVoltageForChangeDetect = currentVoltage;
        return changed;
    }

    // --- Package-private accessors used by unit tests ---

    double getTotalWattHours() {
        return totalWattHours;
    }

    double getLatestVoltage() {
        return latestVoltage;
    }

    double getLatestCurrent() {
        return latestCurrent;
    }
}
