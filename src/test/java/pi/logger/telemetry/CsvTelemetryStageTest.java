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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pi.logger.utils.TimeUtils;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CsvTelemetryStage}.
 *
 * <p>Raw-packet tests use a {@link RawPacketCase} table (see {@link #rawPackets()})
 * so that new packet examples can be added in one place without touching test logic.
 */
class CsvTelemetryStageTest {

    // ---------------------------------------------------------------------------------
    // Raw-packet test table
    // ---------------------------------------------------------------------------------

    /**
     * Describes a raw UDP CSV packet and the exact {@link TelemetryEvent} fields that
     * {@link CsvTelemetryStage#buildEvent} must produce from it.
     */
    record RawPacketCase(
            String raw,
            String expectedChannel,
            TelemetryPayloadType expectedType,
            Object expectedValue,
            long expectedTs) {

        @Override public String toString() { return raw; }
    }

    static Stream<RawPacketCase> rawPackets() {
        return Stream.of(
            new RawPacketCase(
                "1832903929,/Chassis/ActualSpeeds/Vx,double,0,Vx, Vy, Omega",
                "/Chassis/ActualSpeeds/Vx (Vx, Vy, Omega)",
                TelemetryPayloadType.DOUBLE, 0.0, 1832903929L),

            new RawPacketCase(
                "1832903929,/Chassis/FrontLeftModule/ActualState/Speed,double,0,Speed, Angle",
                "/Chassis/FrontLeftModule/ActualState/Speed (Speed, Angle)",
                TelemetryPayloadType.DOUBLE, 0.0, 1832903929L),

            new RawPacketCase(
                "1832903929,/RoboRio/IsBrownOut,bool,false,bool",
                "/RoboRio/IsBrownOut (bool)",
                TelemetryPayloadType.BOOLEAN, false, 1832903929L),

            new RawPacketCase(
                "1832903929,/RoboRio/CPUTemp,double,45,Degrees C",
                "/RoboRio/CPUTemp (Degrees C)",
                TelemetryPayloadType.DOUBLE, 45.0, 1832903929L),

            new RawPacketCase(
                "1832903929,/RoboRio/InputCurrent,double,0,Amps",
                "/RoboRio/InputCurrent (Amps)",
                TelemetryPayloadType.DOUBLE, 0.0, 1832903929L),

            new RawPacketCase(
                "1832903929,/RoboRio/InputVoltage,double,12,Volts",
                "/RoboRio/InputVoltage (Volts)",
                TelemetryPayloadType.DOUBLE, 12.0, 1832903929L),

            new RawPacketCase(
                "1832883890,/Chassis/BackRightModule/TargetState/Angle,double,2.41188,Speed, Angle",
                "/Chassis/BackRightModule/TargetState/Angle (Speed, Angle)",
                TelemetryPayloadType.DOUBLE, 2.41188, 1832883890L)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rawPackets")
    void rawPacket_parsesCorrectly(RawPacketCase tc) {
        String[] parts  = tc.raw().split(",", 5);
        long ts         = TimeUtils.parseTimestampMicros(parts[0].trim());
        String signalId = parts[1].trim();
        String type     = parts[2].trim();
        String value    = parts[3].trim();
        String units    = parts.length > 4 ? parts[4].trim() : "";
        String entryName = units.isEmpty() ? signalId : signalId + " (" + units + ")";

        TelemetryEvent origin = new TelemetryEvent(
                0L, TelemetrySource.UDP, TelemetryPayloadType.CSV, "udp/raw", tc.raw(), null);

        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                origin, ts, entryName, type, value, units, signalId);

        assertNotNull(result, "buildEvent must not return null for a valid packet");
        assertEquals(tc.expectedTs(),      result.timestampUs(), "timestampUs");
        assertEquals(tc.expectedChannel(), result.channel(),     "channel");
        assertEquals(tc.expectedType(),    result.payloadType(), "payloadType");

        if (tc.expectedValue() instanceof Double expected) {
            assertEquals(expected, (Double) result.payload(), 1e-9, "payload value");
        } else {
            assertEquals(tc.expectedValue(), result.payload(), "payload value");
        }
    }

    // ---------------------------------------------------------------------------------
    // buildEvent - array types
    // ---------------------------------------------------------------------------------

    private static TelemetryEvent origin() {
        return new TelemetryEvent(0L, TelemetrySource.UDP,
                TelemetryPayloadType.CSV, "udp/raw", "", null);
    }

    @Test
    void buildEvent_doubleArrayType_producesDoubleArrayEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                origin(), 0L, "Signal", "double_array", "1.0;2.0;3.0", "", "Signal");
        assertEquals(TelemetryPayloadType.DOUBLE_ARRAY, result.payloadType());
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, (double[]) result.payload(), 1e-9);
    }

    @Test
    void buildEvent_pose2dPacket_producesStructEvent() {
        // Raw: 1832903929,/Chassis/Pose2d,double_array,13.9732;3.99347;0.000575243,X, Y, Rotation
        String[] parts   = "1832903929,/Chassis/Pose2d,double_array,13.9732;3.99347;0.000575243,X, Y, Rotation".split(",", 5);
        long ts          = TimeUtils.parseTimestampMicros(parts[0].trim());
        String signalId  = parts[1].trim();
        String type      = parts[2].trim();
        String value     = parts[3].trim();
        String units     = parts[4].trim();
        String entryName = signalId + " (" + units + ")";

        TelemetryEvent origin = new TelemetryEvent(
                0L, TelemetrySource.UDP, TelemetryPayloadType.CSV, "udp/raw", "", null);
        TelemetryEvent result = CsvTelemetryStage.buildEvent(origin, ts, entryName, type, value, units, signalId);

        assertNotNull(result);
        assertEquals(1832903929L,                          result.timestampUs(), "timestampUs");
        assertEquals("/Chassis/Pose2d (X, Y, Rotation)",  result.channel(),     "channel");
        assertEquals(TelemetryPayloadType.STRUCT,          result.payloadType(), "payloadType");

        edu.wpi.first.math.geometry.Pose2d pose =
                assertInstanceOf(edu.wpi.first.math.geometry.Pose2d.class, result.payload(), "payload is Pose2d");
        assertEquals(13.9732,       pose.getX(),                     1e-9, "x");
        assertEquals(3.99347,       pose.getY(),                     1e-9, "y");
        assertEquals(0.000575243,   pose.getRotation().getRadians(), 1e-9, "rotation (radians)");
    }

    @Test
    void buildEvent_questPose2dPacket_allZeros_producesStructEvent() {
        // Raw: 1832883890,/Chassis/QuestPose2d,double_array,0;0;0,X, Y, Rotation
        String[] parts   = "1832883890,/Chassis/QuestPose2d,double_array,0;0;0,X, Y, Rotation".split(",", 5);
        long ts          = TimeUtils.parseTimestampMicros(parts[0].trim());
        String signalId  = parts[1].trim();
        String type      = parts[2].trim();
        String value     = parts[3].trim();
        String units     = parts[4].trim();
        String entryName = signalId + " (" + units + ")";

        TelemetryEvent origin = new TelemetryEvent(
                0L, TelemetrySource.UDP, TelemetryPayloadType.CSV, "udp/raw", "", null);
        TelemetryEvent result = CsvTelemetryStage.buildEvent(origin, ts, entryName, type, value, units, signalId);

        assertNotNull(result);
        assertEquals(1832883890L,                              result.timestampUs(), "timestampUs");
        assertEquals("/Chassis/QuestPose2d (X, Y, Rotation)", result.channel(),     "channel");
        assertEquals(TelemetryPayloadType.STRUCT,              result.payloadType(), "payloadType");

        edu.wpi.first.math.geometry.Pose2d pose =
                assertInstanceOf(edu.wpi.first.math.geometry.Pose2d.class, result.payload(), "payload is Pose2d");
        assertEquals(0.0, pose.getX(),                     1e-9, "x");
        assertEquals(0.0, pose.getY(),                     1e-9, "y");
        assertEquals(0.0, pose.getRotation().getRadians(), 1e-9, "rotation (radians)");
    }

    @Test
    void buildEvent_boolArrayType_producesBooleanArrayEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                origin(), 0L, "Flags", "bool_array", "true;false", "", "Flags");
        assertEquals(TelemetryPayloadType.BOOLEAN_ARRAY, result.payloadType());
        assertArrayEquals(new boolean[]{true, false}, (boolean[]) result.payload());
    }

    @Test
    void buildEvent_joystickButtonsPacket_producesBooleanArrayEvent() {
        // Raw: 1832883890,DS:joystick1/buttons,bool_array,0;0;0;0;0;0;0;0;0;0,
        String[] parts   = "1832883890,DS:joystick1/buttons,bool_array,0;0;0;0;0;0;0;0;0;0,".split(",", 5);
        long ts          = TimeUtils.parseTimestampMicros(parts[0].trim());
        String signalId  = parts[1].trim();
        String type      = parts[2].trim();
        String value     = parts[3].trim();
        String units     = parts[4].trim();
        String entryName = units.isEmpty() ? signalId : signalId + " (" + units + ")";

        TelemetryEvent origin = new TelemetryEvent(
                0L, TelemetrySource.UDP, TelemetryPayloadType.CSV, "udp/raw", "", null);
        TelemetryEvent result = CsvTelemetryStage.buildEvent(origin, ts, entryName, type, value, units, signalId);

        assertNotNull(result);
        assertEquals(1832883890L,                       result.timestampUs(), "timestampUs");
        assertEquals("DS:joystick1/buttons",            result.channel(),     "channel");
        assertEquals(TelemetryPayloadType.BOOLEAN_ARRAY, result.payloadType(), "payloadType");
        assertArrayEquals(
                new boolean[]{false, false, false, false, false, false, false, false, false, false},
                (boolean[]) result.payload(), "payload values");
    }

    @Test
    void buildEvent_joystick0ButtonsPacket_producesBooleanArrayEvent() {
        // Raw: 184816,DS:joystick0/buttons,bool_array,0;1;0;1;0;0;0;0;0;0,
        String[] parts   = "184816,DS:joystick0/buttons,bool_array,0;1;0;1;0;0;0;0;0;0,".split(",", 5);
        long ts          = TimeUtils.parseTimestampMicros(parts[0].trim());
        String signalId  = parts[1].trim();
        String type      = parts[2].trim();
        String value     = parts[3].trim();
        String units     = parts[4].trim();
        String entryName = units.isEmpty() ? signalId : signalId + " (" + units + ")";

        TelemetryEvent origin = new TelemetryEvent(
                0L, TelemetrySource.UDP, TelemetryPayloadType.CSV, "udp/raw", "", null);
        TelemetryEvent result = CsvTelemetryStage.buildEvent(origin, ts, entryName, type, value, units, signalId);

        assertNotNull(result);
        assertEquals(184816L,                           result.timestampUs(), "timestampUs");
        assertEquals("DS:joystick0/buttons",            result.channel(),     "channel");
        assertEquals(TelemetryPayloadType.BOOLEAN_ARRAY, result.payloadType(), "payloadType");
        assertArrayEquals(
                new boolean[]{false, true, false, true, false, false, false, false, false, false},
                (boolean[]) result.payload(), "payload values");
    }

    @Test
    void buildEvent_intArrayType_producesIntegerArrayEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                origin(), 0L, "Counts", "int_array", "1;2;3", "", "Counts");
        assertEquals(TelemetryPayloadType.INTEGER_ARRAY, result.payloadType());
        assertArrayEquals(new long[]{1L, 2L, 3L}, (long[]) result.payload());
    }

    @Test
    void buildEvent_floatArrayType_producesFloatArrayEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                origin(), 0L, "Floats", "float_array", "1.0;2.0", "", "Floats");
        assertEquals(TelemetryPayloadType.FLOAT_ARRAY, result.payloadType());
        assertArrayEquals(new float[]{1.0f, 2.0f}, (float[]) result.payload(), 1e-6f);
    }

    @Test
    void buildEvent_joystickAxesPacket_producesFloatArrayEvent() {
        // Raw: 1832883890,DS:joystick1/axes,float_array,0;-0;0;0;0;-0,
        String[] parts   = "1832883890,DS:joystick1/axes,float_array,0;-0;0;0;0;-0,".split(",", 5);
        long ts          = TimeUtils.parseTimestampMicros(parts[0].trim());
        String signalId  = parts[1].trim();
        String type      = parts[2].trim();
        String value     = parts[3].trim();
        String units     = parts[4].trim();
        String entryName = units.isEmpty() ? signalId : signalId + " (" + units + ")";

        TelemetryEvent origin = new TelemetryEvent(
                0L, TelemetrySource.UDP, TelemetryPayloadType.CSV, "udp/raw", "", null);
        TelemetryEvent result = CsvTelemetryStage.buildEvent(origin, ts, entryName, type, value, units, signalId);

        assertNotNull(result);
        assertEquals(1832883890L,                      result.timestampUs(), "timestampUs");
        assertEquals("DS:joystick1/axes",              result.channel(),     "channel");
        assertEquals(TelemetryPayloadType.FLOAT_ARRAY, result.payloadType(), "payloadType");
        assertArrayEquals(new float[]{0f, 0f, 0f, 0f, 0f, 0f},
                (float[]) result.payload(), 1e-6f, "payload values");
    }

    @Test
    void buildEvent_joystick0AxesPacket_producesFloatArrayEvent() {
        // Raw: 1832883890,DS:joystick0/axes,float_array,-0;-0;0;0;-0;0,
        String[] parts   = "1832883890,DS:joystick0/axes,float_array,-0;-0;0;0;-0;0,".split(",", 5);
        long ts          = TimeUtils.parseTimestampMicros(parts[0].trim());
        String signalId  = parts[1].trim();
        String type      = parts[2].trim();
        String value     = parts[3].trim();
        String units     = parts[4].trim();
        String entryName = units.isEmpty() ? signalId : signalId + " (" + units + ")";

        TelemetryEvent origin = new TelemetryEvent(
                0L, TelemetrySource.UDP, TelemetryPayloadType.CSV, "udp/raw", "", null);
        TelemetryEvent result = CsvTelemetryStage.buildEvent(origin, ts, entryName, type, value, units, signalId);

        assertNotNull(result);
        assertEquals(1832883890L,                      result.timestampUs(), "timestampUs");
        assertEquals("DS:joystick0/axes",              result.channel(),     "channel");
        assertEquals(TelemetryPayloadType.FLOAT_ARRAY, result.payloadType(), "payloadType");
        assertArrayEquals(new float[]{0f, 0f, 0f, 0f, 0f, 0f},
                (float[]) result.payload(), 1e-6f, "payload values");
    }

    // ---------------------------------------------------------------------------------
    // buildEvent - bad input
    // ---------------------------------------------------------------------------------

    @Test
    void buildEvent_unknownType_fallsBackToString() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                origin(), 0L, "X", "quaternion", "1,0,0,0", "", "X");
        assertNotNull(result);
        assertEquals(TelemetryPayloadType.STRING, result.payloadType(), "payloadType");
        assertEquals("1,0,0,0", result.payload(), "payload value");
    }

    @Test
    void buildEvent_unparsableDoubleValue_returnsNull() {
        assertNull(CsvTelemetryStage.buildEvent(
                origin(), 0L, "X", "double", "not-a-number", "", "X"));
    }

    // ---------------------------------------------------------------------------------
    // apply() - guard conditions
    // ---------------------------------------------------------------------------------

    private static TelemetryContext csvContext(String payload) {
        return new TelemetryContext(new TelemetryEvent(
                0L, TelemetrySource.UDP, TelemetryPayloadType.CSV, "udp/raw", payload, null));
    }

    @Test
    void apply_nonCsvPayloadType_doesNotThrow() {
        TelemetryContext ctx = new TelemetryContext(new TelemetryEvent(
                0L, TelemetrySource.UDP, TelemetryPayloadType.DOUBLE, "udp/raw", 42.0, null));
        assertDoesNotThrow(() -> new CsvTelemetryStage().apply(ctx));
    }

    @Test
    void apply_tooFewFields_doesNotThrow() {
        assertDoesNotThrow(() -> new CsvTelemetryStage().apply(csvContext("bad,data")));
    }

    @Test
    void apply_emptyPayload_doesNotThrow() {
        assertDoesNotThrow(() -> new CsvTelemetryStage().apply(csvContext("")));
    }

    @Test
    void apply_nullPayload_doesNotThrow() {
        assertDoesNotThrow(() -> new CsvTelemetryStage().apply(csvContext(null)));
    }
}