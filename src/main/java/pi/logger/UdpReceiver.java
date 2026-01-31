package pi.logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class UdpReceiver {

    private static final int PORT = 5900; // choose your port
    private static final int MAX_PACKET_SIZE = 1500;

    private static final BlockingQueue<UdpMessage> queue =
            new LinkedBlockingQueue<>(10_000);

    private static volatile boolean running = true;
    private static volatile DatagramSocket socket = null;
    private static volatile long messagesProcessed = 0;

    private UdpReceiver() {}

    public static void start() {
        Thread t = new Thread(UdpReceiver::run, "udp-receiver");
        t.setDaemon(true);
        t.start();
    }

    public static void stop() {
        running = false;
        // Close the socket to unblock the receive() call
        DatagramSocket s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (Exception e) {
                // Ignore exceptions during close
            }
        }
    }

    public static BlockingQueue<UdpMessage> getQueue() {
        return queue;
    }

    public static long getMessagesProcessed() {
        return messagesProcessed;
    }

    private static void run() {
        DatagramSocket s = null;
        try {
            s = new DatagramSocket(null);
            socket = s;
            s.bind(new InetSocketAddress(PORT));
            s.setReceiveBufferSize(1 << 20); // 1 MB buffer

            System.out.println("UDP receiver listening on port " + PORT);

            byte[] buffer = new byte[MAX_PACKET_SIZE];

            while (running) {
                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length);

                s.receive(packet); // blocking

                long timestamp = System.nanoTime();

                byte[] payload = new byte[packet.getLength()];
                System.arraycopy(
                        packet.getData(),
                        packet.getOffset(),
                        payload,
                        0,
                        packet.getLength()
                );

                UdpMessage msg = new UdpMessage(
                        timestamp,
                        payload
                );

                if (queue.offer(msg)) { // non-blocking
                    messagesProcessed++;
                }
            }
        } catch (SocketException e) {
            // Expected when socket is closed by stop()
            if (running) {
                System.err.println("UDP receiver socket error");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("UDP receiver error");
            e.printStackTrace();
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
            } catch (Exception e) {
                // Ignore exceptions during close
            }
            socket = null;
        }
    }
}

