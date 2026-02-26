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

import pi.logger.datalog.USBFileLogger;

import edu.wpi.first.util.struct.Struct;

public final class DataLogStage implements TelemetryStage {
    @Override
    public void apply(TelemetryContext context) {
        TelemetryEvent event = context.getEvent();
        if (event == null) {
            return;
        }

        switch (event.payloadType()) {
            case DOUBLE -> logDouble(event);
            case INTEGER -> logInteger(event);
            case BOOLEAN -> logBoolean(event);
            case STRING -> logString(event);
            case STRUCT -> logStruct(event);
            case STRUCT_ARRAY -> logStructArray(event);
            case BOOLEAN_ARRAY -> logBooleanArray(event);
            case INTEGER_ARRAY -> logIntegerArray(event);
            case DOUBLE_ARRAY -> logDoubleArray(event);
            case FLOAT_ARRAY -> logFloatArray(event);
            default -> {
                // ignore unsupported types here
            }
        }
    }

    private void logDouble(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof Number number) {
            USBFileLogger.logDouble(event.channel(), number.doubleValue(), event.timestampUs());
        }
    }

    private void logInteger(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof Number number) {
            USBFileLogger.logInteger(event.channel(), number.longValue(), event.timestampUs());
        }
    }

    private void logBoolean(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof Boolean bool) {
            USBFileLogger.logBoolean(event.channel(), bool, event.timestampUs());
        }
    }

    private void logString(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload != null) {
            USBFileLogger.logString(event.channel(), payload.toString(), event.timestampUs());
        }
    }

    private void logBooleanArray(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof boolean[] boolArray) {
            USBFileLogger.logBooleanArray(event.channel(), boolArray, event.timestampUs());
        }
    }

    private void logIntegerArray(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof long[] longArray) {
            USBFileLogger.logIntegerArray(event.channel(), longArray, event.timestampUs());
        }
    }

    private void logDoubleArray(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof double[] doubleArray) {
            USBFileLogger.logDoubleArray(event.channel(), doubleArray, event.timestampUs());
        }
    }

    private void logFloatArray(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof float[] floatArray) {
            USBFileLogger.logFloatArray(event.channel(), floatArray, event.timestampUs());
        }
    }

    @SuppressWarnings("unchecked")
    private void logStruct(TelemetryEvent event) {
        Struct<Object> struct = (Struct<Object>) event.structSchema();
        if (struct == null || event.payload() == null) {
            return;
        }
        USBFileLogger.logStructEntry(event.channel(), event.payload(), struct, event.timestampUs());
    }

    @SuppressWarnings("unchecked")
    private void logStructArray(TelemetryEvent event) {
        Struct<Object> struct = (Struct<Object>) event.structSchema();
        if (struct == null || event.payload() == null) {
            return;
        }
        Object payload = event.payload();
        if (payload instanceof Object[] array) {
            USBFileLogger.logStructArray(event.channel(), array, struct, event.timestampUs());
        }
    }
}
