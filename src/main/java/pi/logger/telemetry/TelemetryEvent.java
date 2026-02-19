package pi.logger.telemetry;

import edu.wpi.first.util.struct.Struct;

public record TelemetryEvent(
        long timestampNs,
        TelemetrySource source,
        TelemetryPayloadType payloadType,
        String channel,
        Object payload,
        Struct<?> structSchema
) {
    public TelemetryEvent {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel must be provided");
        }
        if (source == null || payloadType == null) {
            throw new IllegalArgumentException("source and payloadType must be provided");
        }
    }
}
