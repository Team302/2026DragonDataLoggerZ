package pi.logger;

/**
 * Simple wrapper to run the UdpSender from the jar with a clear main entry.
 * Usage: java pi.logger.SenderMain <host> <port> [count] [intervalMs] [timestamped]
 */
public class SenderMain {
    public static void main(String[] args) throws Exception {
        UdpSender.main(args);
    }
}
