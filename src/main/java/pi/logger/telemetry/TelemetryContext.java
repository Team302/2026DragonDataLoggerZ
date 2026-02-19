package pi.logger.telemetry;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TelemetryContext {
    private final TelemetryEvent event;
    private final Map<String, Object> attributes = new HashMap<>();
    private String cachedStringPayload;

    public TelemetryContext(TelemetryEvent event) {
        this.event = event;
    }

    public TelemetryEvent getEvent() {
        return event;
    }

    public long timestampNs() {
        return event.timestampNs();
    }

    public String channel() {
        return event.channel();
    }

    public TelemetryPayloadType payloadType() {
        return event.payloadType();
    }

    public Object payload() {
        return event.payload();
    }

    public String payloadAsString() {
        if (event.payload() == null) {
            return null;
        }
        if (event.payload() instanceof String s) {
            return s;
        }
        if (event.payload() instanceof byte[] bytes) {
            if (cachedStringPayload == null) {
                cachedStringPayload = new String(bytes, StandardCharsets.UTF_8);
            }
            return cachedStringPayload;
        }
        return event.payload().toString();
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, Object> attributesView() {
        return Collections.unmodifiableMap(attributes);
    }
}
