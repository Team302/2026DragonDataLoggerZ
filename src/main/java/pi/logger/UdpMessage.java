package pi.logger;

public record UdpMessage(
        long timestampNs,
        byte[] payload
) {}

