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

import edu.wpi.first.math.geometry.Pose2d;
import pi.logger.config.LoggerConfig;
import pi.logger.csvparsers.Pose2dUtil;
import pi.logger.utils.TimeUtils;

public final class CsvTelemetryStage implements TelemetryStage {

    /**
     * When {@code true}, the timestamp parsed from the CSV payload (parts[0]) is used
     * as the WPILOG event timestamp.  When {@code false}, {@link TimeUtils#nowUs()} is
     * used instead (the Pi's local receive time).  Controlled by
     * {@code csv.usePayloadTimestamp} in {@code logger.properties}.
     */
    private static final boolean USE_PAYLOAD_TIMESTAMP =
            LoggerConfig.getBoolean("csv.usePayloadTimestamp", true);

    @Override
    public void apply(TelemetryContext context)
    {
        if (context.payloadType() != TelemetryPayloadType.CSV) {
            return;
        }
        String payload = context.payloadAsString();
        if (payload == null) {
            return;
        }

        // Parse CSV format: timestamp,signalID,type,value,units
        String[] parts = payload.split(",", 5);
        if (parts.length < 4) {
            System.err.println("Invalid message format: " + payload);
            return;
        }

        // Parse the CSV timestamp (parts[0]) as microseconds for the WPILOG file.
        // Only parse (and only emit parse-error logs) when the result will actually be used.
        long timestampMicros = USE_PAYLOAD_TIMESTAMP
                ? TimeUtils.parseTimestampMicros(parts[0].trim())
                : TimeUtils.nowUs();
        String signalId = parts[1].trim();
        String type = parts[2].trim();
        String value = parts[3].trim();
        String units = parts.length > 4 ? parts[4].trim() : "";
        String entryName = units.isEmpty() ? signalId : signalId + " (" + units + ")";

        TelemetryEvent original = context.getEvent();

        TelemetryEvent outEvent = buildEvent(original, timestampMicros, entryName, type, value, units, signalId);
        if (outEvent != null) {
            TelemetryProcessor.publish(outEvent);
        }
    }

    /**
     * Converts the parsed CSV fields into a typed {@link TelemetryEvent} that
     * {@link DataLogStage} will write to the WPILOG file.  Returns {@code null}
     * if the value cannot be parsed.
     * <p>Package-private to allow direct testing without a running processor.</p>
     */
    static TelemetryEvent buildEvent(
            TelemetryEvent original,
            long timestampMicros,
            String entryName,
            String type,
            String value,
            String units,
            String signalId) {

        try {
            return switch (type.toLowerCase()) {
                case "double", "float" -> new TelemetryEvent(
                        timestampMicros,
                        original.source(),
                        TelemetryPayloadType.DOUBLE,
                        entryName,
                        Double.parseDouble(value),
                        null);

                case "int", "integer", "long" -> new TelemetryEvent(
                        timestampMicros,
                        original.source(),
                        TelemetryPayloadType.INTEGER,
                        entryName,
                        Long.parseLong(value),
                        null);

                case "bool", "boolean" -> new TelemetryEvent(
                        timestampMicros,
                        original.source(),
                        TelemetryPayloadType.BOOLEAN,
                        entryName,
                        Boolean.parseBoolean(value),
                        null);

                case "string" -> new TelemetryEvent(
                        timestampMicros,
                        original.source(),
                        TelemetryPayloadType.STRING,
                        entryName,
                        value,
                        null);

                case "bool_array" -> new TelemetryEvent(
                        timestampMicros,
                        original.source(),
                        TelemetryPayloadType.BOOLEAN_ARRAY,
                        entryName,
                        TelemetryArrayHelper.getBooleanArray(value),
                        null);

                case "int_array" -> new TelemetryEvent(
                        timestampMicros,
                        original.source(),
                        TelemetryPayloadType.INTEGER_ARRAY,
                        entryName,
                        TelemetryArrayHelper.getIntArray(value),
                        null);

                case "double_array" -> {
                    if (signalId.toLowerCase().contains("pose2d")) {
                        Pose2d pose = Pose2dUtil.fromString(value);
                        yield new TelemetryEvent(
                                timestampMicros,
                                original.source(),
                                TelemetryPayloadType.STRUCT,
                                entryName,
                                pose,
                                Pose2d.struct);
                    } else {
                        yield new TelemetryEvent(
                                timestampMicros,
                                original.source(),
                                TelemetryPayloadType.DOUBLE_ARRAY,
                                entryName,
                                TelemetryArrayHelper.getDoubleArray(value),
                                null);
                    }
                }

                case "float_array" -> new TelemetryEvent(
                        timestampMicros,
                        original.source(),
                        TelemetryPayloadType.FLOAT_ARRAY,
                        entryName,
                        TelemetryArrayHelper.getFloatArray(value),
                        null);

                default -> {
                    System.err.println("Unknown CSV type: " + type + " for entry: " + entryName + " (falling back to STRING)");
                    yield new TelemetryEvent(
                            timestampMicros,
                            original.source(),
                            TelemetryPayloadType.STRING,
                            entryName,
                            value,
                            null);
                }
            };
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse CSV value for type " + type + ": " + value);
            return null;
        }
    }

}
