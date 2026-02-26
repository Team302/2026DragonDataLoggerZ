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
import pi.logger.utils.TimeUtils;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link CsvTelemetryStage} using the real-world UDP packet format:
 *
 * <pre>Em\t1726935951,/Chassis/FrontLeftModule/TargetState/Angle,double,1.36562,Speed, Angle</pre>
 *
 * The leading {@code Em\t} bytes are raw UDP framing prepended by the robot sender.
 * {@code CsvTelemetryStage} parses the CSV fields and publishes a typed
 * {@link TelemetryEvent} that {@link DataLogStage} will forward to USBFileLogger —
 * verified here by calling the package-private {@link CsvTelemetryStage#buildEvent}
 * directly.
 */
class CsvTelemetryStageTest {

    /** Real-world raw UDP payload produced by the robot sender. */
    static final String RAW_PACKET =
            "1726935951,/Chassis/FrontLeftModule/TargetState/Angle,double,1.36562,Speed, Angle";

    // ── factory helpers ─────────────────────────────────────────────────────────

    private static TelemetryEvent originEvent() {
        return new TelemetryEvent(
                0L,
                TelemetrySource.UDP,
                TelemetryPayloadType.CSV,
                "udp/raw",
                RAW_PACKET,
                null);
    }

    private static TelemetryContext csvContext(String payload) {
        TelemetryEvent event = new TelemetryEvent(
                0L,
                TelemetrySource.UDP,
                TelemetryPayloadType.CSV,
                "udp/raw",
                payload,
                null);
        return new TelemetryContext(event);
    }

    private static TelemetryContext nonCsvContext() {
        TelemetryEvent event = new TelemetryEvent(
                0L,
                TelemetrySource.UDP,
                TelemetryPayloadType.DOUBLE,
                "udp/raw",
                42.0,
                null);
        return new TelemetryContext(event);
    }


    // ── buildEvent – real-world double packet ───────────────────────────────────

    @Test
    void buildEvent_doubleType_producesDoubleEvent() {
        String[] parts = RAW_PACKET.split(",", 5);
        long ts       = TimeUtils.parseTimestampMicros(parts[0].trim());
        String signalId = parts[1].trim();              // /Chassis/FrontLeftModule/TargetState/Angle
        String type     = parts[2].trim();              // double
        String value    = parts[3].trim();              // 1.36562
        String units    = parts[4].trim();              // Speed, Angle
        String entryName = signalId + " (" + units + ")";

        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), ts, entryName, type, value, units, signalId);

        assertNotNull(result, "buildEvent must return a non-null event for a valid double packet");
        assertEquals(TelemetryPayloadType.DOUBLE, result.payloadType());
        assertEquals("/Chassis/FrontLeftModule/TargetState/Angle (Speed, Angle)", result.channel());
        assertEquals(1.36562, (Double) result.payload(), 1e-9);
        assertEquals(1726935951L, result.timestampUs(),
                "timestamp should equal the value parsed from the payload");
    }

    @Test
    void buildEvent_doubleType_valueIsCorrect() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L,
                "/Chassis/FrontLeftModule/TargetState/Angle (Speed, Angle)",
                "double", "1.36562", "Speed, Angle",
                "/Chassis/FrontLeftModule/TargetState/Angle");

        assertInstanceOf(Double.class, result.payload());
        assertEquals(1.36562, (Double) result.payload(), 1e-9);
    }

    @Test
    void buildEvent_units_appendedToChannel() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L,
                "/Chassis/FrontLeftModule/TargetState/Angle (Speed, Angle)",
                "double", "1.36562", "Speed, Angle",
                "/Chassis/FrontLeftModule/TargetState/Angle");

        assertTrue(result.channel().contains("Speed, Angle"),
                "units including the internal comma must appear in the channel name");
    }

    @Test
    void buildEvent_sourcePreservedFromOriginal() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L,
                "/Chassis/FrontLeftModule/TargetState/Angle",
                "double", "1.36562", "",
                "/Chassis/FrontLeftModule/TargetState/Angle");

        assertEquals(TelemetrySource.UDP, result.source());
    }

    // ── buildEvent – other scalar types ─────────────────────────────────────────

    @Test
    void buildEvent_integerType_producesIntegerEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 100L, "Counter", "int", "42", "", "Counter");

        assertEquals(TelemetryPayloadType.INTEGER, result.payloadType());
        assertEquals(42L, (Long) result.payload());
    }

    @Test
    void buildEvent_booleanType_producesBooleanEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L, "Flag", "bool", "true", "", "Flag");

        assertEquals(TelemetryPayloadType.BOOLEAN, result.payloadType());
        assertTrue((Boolean) result.payload());
    }

    @Test
    void buildEvent_stringType_producesStringEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L, "Label", "string", "hello", "", "Label");

        assertEquals(TelemetryPayloadType.STRING, result.payloadType());
        assertEquals("hello", result.payload());
    }

    // ── buildEvent – array types ─────────────────────────────────────────────────

    @Test
    void buildEvent_doubleArrayType_producesDoubleArrayEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L, "Signal", "double_array", "1.0;2.0;3.0", "", "Signal");

        assertEquals(TelemetryPayloadType.DOUBLE_ARRAY, result.payloadType());
        assertInstanceOf(double[].class, result.payload());
    }

    @Test
    void buildEvent_boolArrayType_producesBooleanArrayEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L, "Flags", "bool_array", "true;false", "", "Flags");

        assertEquals(TelemetryPayloadType.BOOLEAN_ARRAY, result.payloadType());
        assertInstanceOf(boolean[].class, result.payload());
    }

    @Test
    void buildEvent_intArrayType_producesIntegerArrayEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L, "Counts", "int_array", "1;2;3", "", "Counts");

        assertEquals(TelemetryPayloadType.INTEGER_ARRAY, result.payloadType());
        assertInstanceOf(long[].class, result.payload());
    }

    @Test
    void buildEvent_floatArrayType_producesFloatArrayEvent() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L, "Floats", "float_array", "1.0;2.0", "", "Floats");

        assertEquals(TelemetryPayloadType.FLOAT_ARRAY, result.payloadType());
        assertInstanceOf(float[].class, result.payload());
    }

    // ── buildEvent – bad input ────────────────────────────────────────────────────

    @Test
    void buildEvent_unknownType_returnsNull() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L, "X", "quaternion", "1,0,0,0", "", "X");
        assertNull(result);
    }

    @Test
    void buildEvent_unparsableDoubleValue_returnsNull() {
        TelemetryEvent result = CsvTelemetryStage.buildEvent(
                originEvent(), 0L, "X", "double", "not-a-number", "", "X");
        assertNull(result);
    }

    // ── apply() – guard conditions ───────────────────────────────────────────────

    @Test
    void apply_nonCsvPayloadType_doesNotThrow() {
        assertDoesNotThrow(() -> new CsvTelemetryStage().apply(nonCsvContext()));
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
