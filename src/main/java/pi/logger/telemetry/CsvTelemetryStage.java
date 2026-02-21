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
import pi.logger.datalog.Pose2dUtil;
import pi.logger.datalog.USBFileLogger;

public final class CsvTelemetryStage implements TelemetryStage {
    @Override
    public void apply(TelemetryContext context) {
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

        String signalId = parts[1].trim();
        String type = parts[2].trim();
        String value = parts[3].trim();
        String units = parts.length > 4 ? parts[4].trim() : "";

        if (!"pose2d".equalsIgnoreCase(type)) {
            USBFileLogger.logCsvPayload(payload);
            return;
        }

        String entryName = units.isEmpty() ? signalId : signalId + " (" + units + ")";
        boolean rotInDegrees = units.toLowerCase().contains("deg");

        try {
            String[] coords = value.split(";", 3);
            if (coords.length < 3) {
                System.err.println("Invalid Pose2d value format (expected 'x;y;rot'): " + value);
                return;
            }
            double x = Double.parseDouble(coords[0].trim());
            double y = Double.parseDouble(coords[1].trim());
            double rot = Double.parseDouble(coords[2].trim());
            Pose2d pose = Pose2dUtil.fromArray(new double[]{x, y, rot}, rotInDegrees);

            TelemetryEvent original = context.getEvent();
            TelemetryEvent poseEvent = new TelemetryEvent(
                    original.timestampNs(),
                    original.source(),
                    TelemetryPayloadType.STRUCT,
                    entryName,
                    pose,
                    Pose2d.struct
            );
            TelemetryProcessor.publish(poseEvent);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse Pose2d: " + value);
        }
    }
}
