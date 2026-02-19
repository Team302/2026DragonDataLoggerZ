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
            default -> {
                // ignore unsupported types here
            }
        }
    }

    private void logDouble(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof Number number) {
            USBFileLogger.logDouble(event.channel(), number.doubleValue());
        }
    }

    private void logInteger(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof Number number) {
            USBFileLogger.logInteger(event.channel(), number.longValue());
        }
    }

    private void logBoolean(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload instanceof Boolean bool) {
            USBFileLogger.logBoolean(event.channel(), bool);
        }
    }

    private void logString(TelemetryEvent event) {
        Object payload = event.payload();
        if (payload != null) {
            USBFileLogger.logString(event.channel(), payload.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private void logStruct(TelemetryEvent event) {
        Struct<Object> struct = (Struct<Object>) event.structSchema();
        if (struct == null || event.payload() == null) {
            return;
        }
        USBFileLogger.logStructEntry(event.channel(), event.payload(), struct);
    }

    @SuppressWarnings("unchecked")
    private void logStructArray(TelemetryEvent event) {
        Struct<Object> struct = (Struct<Object>) event.structSchema();
        if (struct == null || event.payload() == null) {
            return;
        }
        Object payload = event.payload();
        if (payload instanceof Object[] array) {
            USBFileLogger.logStructArray(event.channel(), array, struct);
        }
    }
}
