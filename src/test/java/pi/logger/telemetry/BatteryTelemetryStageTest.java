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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatteryTelemetryStageTest {

    private BatteryTelemetryStage stage;

    @BeforeEach
    void setUp() {
        stage = new BatteryTelemetryStage();
    }

    // ── computeWatts ────────────────────────────────────────────────────────────

    @Test
    void computeWatts_returnsVoltageTimedCurrent() {
        assertEquals(120.0, BatteryTelemetryStage.computeWatts(12.0, 10.0), 1e-9);
    }

    @Test
    void computeWatts_zeroVoltage_returnsZero() {
        assertEquals(0.0, BatteryTelemetryStage.computeWatts(0.0, 5.0), 1e-9);
    }

    @Test
    void computeWatts_zeroCurrent_returnsZero() {
        assertEquals(0.0, BatteryTelemetryStage.computeWatts(12.6, 0.0), 1e-9);
    }

    // ── updateReading ───────────────────────────────────────────────────────────

    @Test
    void updateReading_voltageChannel_storesVoltage() {
        stage.updateReading(BatteryTelemetryStage.DEFAULT_VOLTAGE_CHANNEL, 12.5);
        assertEquals(12.5, stage.getLatestVoltage(), 1e-9);
    }

    @Test
    void updateReading_currentChannel_storesCurrent() {
        stage.updateReading(BatteryTelemetryStage.DEFAULT_CURRENT_CHANNEL, 3.2);
        assertEquals(3.2, stage.getLatestCurrent(), 1e-9);
    }

    @Test
    void updateReading_unknownChannel_doesNotAlterState() {
        stage.updateReading("Other/Channel", 99.0);
        assertTrue(Double.isNaN(stage.getLatestVoltage()));
        assertTrue(Double.isNaN(stage.getLatestCurrent()));
    }

    // ── watt-hour accumulation ───────────────────────────────────────────────────

    @Test
    void maybeComputeAndLog_accumulatesWattHoursOverTime() {
        stage.updateReading(BatteryTelemetryStage.DEFAULT_VOLTAGE_CHANNEL, 12.0);
        stage.updateReading(BatteryTelemetryStage.DEFAULT_CURRENT_CHANNEL, 10.0);

        long t0 = 0L;
        long t1 = t0 + 2000L; // 2 seconds later

        // First call initialises the timer (no elapsed time yet → no computation)
        stage.maybeComputeAndLog(t0);
        assertEquals(0.0, stage.getTotalWattHours(), 1e-9);

        // Second call is 2 s later → should accumulate (120 W × 2/3600 h)
        stage.maybeComputeAndLog(t1);
        double expectedWh = 120.0 * 2.0 / 3600.0;
        assertEquals(expectedWh, stage.getTotalWattHours(), 1e-6);
    }

    @Test
    void maybeComputeAndLog_noComputationBeforeOneSecond() {
        stage.updateReading(BatteryTelemetryStage.DEFAULT_VOLTAGE_CHANNEL, 12.0);
        stage.updateReading(BatteryTelemetryStage.DEFAULT_CURRENT_CHANNEL, 10.0);

        stage.maybeComputeAndLog(0L);
        stage.maybeComputeAndLog(500L); // only 500 ms → should not compute
        assertEquals(0.0, stage.getTotalWattHours(), 1e-9);
    }

    @Test
    void maybeComputeAndLog_missingReadings_doesNotAccumulate() {
        // Only voltage, no current
        stage.updateReading(BatteryTelemetryStage.DEFAULT_VOLTAGE_CHANNEL, 12.0);

        stage.maybeComputeAndLog(0L);
        stage.maybeComputeAndLog(2000L);
        assertEquals(0.0, stage.getTotalWattHours(), 1e-9);
    }

    // ── battery change detection ─────────────────────────────────────────────────

    @Test
    void detectBatteryChange_firstCall_returnsFalse() {
        assertFalse(stage.detectBatteryChange(12.0));
    }

    @Test
    void detectBatteryChange_smallIncrease_returnsFalse() {
        stage.detectBatteryChange(12.0); // initialise baseline
        assertFalse(stage.detectBatteryChange(12.1)); // below threshold
    }

    @Test
    void detectBatteryChange_largeIncrease_returnsTrue() {
        stage.detectBatteryChange(6.0); // voltage after near-zero (power reset)
        assertTrue(stage.detectBatteryChange(12.6)); // 6.6 V jump ≥ threshold
    }

    @Test
    void detectBatteryChange_exactThreshold_returnsTrue() {
        double threshold = BatteryTelemetryStage.BATTERY_CHANGE_THRESHOLD_VOLTS;
        stage.detectBatteryChange(10.0);
        assertTrue(stage.detectBatteryChange(10.0 + threshold));
    }

    @Test
    void detectBatteryChange_justBelowThreshold_returnsFalse() {
        double threshold = BatteryTelemetryStage.BATTERY_CHANGE_THRESHOLD_VOLTS;
        stage.detectBatteryChange(10.0);
        assertFalse(stage.detectBatteryChange(10.0 + threshold - 0.001));
    }

    @Test
    void detectBatteryChange_voltageDecrease_returnsFalse() {
        stage.detectBatteryChange(13.0);
        assertFalse(stage.detectBatteryChange(11.0)); // drop, not a new battery
    }
}
