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

    public long timestampUs() {
        return event.timestampUs();
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
