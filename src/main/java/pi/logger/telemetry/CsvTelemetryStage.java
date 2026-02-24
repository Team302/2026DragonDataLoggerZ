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
import pi.logger.csvparsers.Pose2dUtil;
import pi.logger.datalog.USBFileLogger;

public final class CsvTelemetryStage implements TelemetryStage {
    @Override
    public void apply(TelemetryContext context) 
    {
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
        // The robot sends a numeric timestamp; fall back to 0 if unparseable.
        long timestampMicros = parseTimestampMicros(parts[0].trim());

        String signalId = parts[1].trim();
        String type = parts[2].trim();
        String value = parts[3].trim();
        String units = parts.length > 4 ? parts[4].trim() : "";

        if ("bool_array".equalsIgnoreCase(type))
        {
            USBFileLogger.logBooleanArray(signalId, TelemetryArrayHelper.getBooleanArray(value), timestampMicros);
        }
        else if ("int_array".equalsIgnoreCase(type))
        {
            USBFileLogger.logIntegerArray(signalId,TelemetryArrayHelper.getIntArray(value), timestampMicros);
        }
        else if ("double_array".equalsIgnoreCase(type))
        {
            USBFileLogger.logDoubleArray(signalId,TelemetryArrayHelper.getDoubleArray(value), timestampMicros);
        }
        else if ("float_array".equalsIgnoreCase(type))
        {
            USBFileLogger.logFloatArray(signalId,TelemetryArrayHelper.getFloatArray(value), timestampMicros);
        }
        else if (signalId.toLowerCase().contains("pose2d")) {
            Pose2d pose = Pose2dUtil.fromString(value);
            String entryName = units.isEmpty() ? signalId : signalId + " (" + units + ")";
            //System.out.println("Parsed Pose2d from CSV: " + signalId + " = " + pose);
            TelemetryEvent original = context.getEvent();
            TelemetryEvent poseEvent = new TelemetryEvent(
                    original.timestampUs(),
                    original.source(),
                    TelemetryPayloadType.STRUCT,
                    entryName,
                    pose,
                    Pose2d.struct
            );
            TelemetryProcessor.publish(poseEvent);
            
        } else {
            USBFileLogger.logCsvPayload(payload, context.getEvent().timestampUs());
        }
        
    }
    /**
     * Parse the CSV timestamp string into microseconds for the WPILOG file.
     * Accepts integer microseconds directly. If the value contains a decimal
     * point it is treated as seconds and converted to microseconds.
     * Returns 0 on any parse failure so logging still proceeds.
     */
    private static long parseTimestampMicros(String raw) {
        if (raw == null || raw.isEmpty()) {
            return 0;
        }
        try {
            if (raw.contains(".")) {
                // Treat as seconds (e.g. FPGA timestamp from WPILib Timer.getFPGATimestamp())
                double seconds = Double.parseDouble(raw);
                return (long) (seconds * 1_000_000);
            }
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse CSV timestamp: " + raw);
            return 0;
        }
    }
}
