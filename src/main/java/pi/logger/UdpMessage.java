package pi.logger;

public record UdpMessage(
        long timestampNs,
        String sourceIp,
        int sourcePort,
        byte[] payload
) {}

