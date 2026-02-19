package pi.logger.telemetry;

@FunctionalInterface
public interface TelemetryStage {
    void apply(TelemetryContext context) throws Exception;
}
