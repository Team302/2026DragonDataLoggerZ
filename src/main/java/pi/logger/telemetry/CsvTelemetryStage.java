package pi.logger.telemetry;

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
        USBFileLogger.logCsvPayload(payload);
    }
}
