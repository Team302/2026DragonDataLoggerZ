package pi.logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class UdpReceiver {

    private static final int PORT = 5900; // choose your port
    private static final int MAX_PACKET_SIZE = 1500;

    private static final BlockingQueue<UdpMessage> queue =
            new LinkedBlockingQueue<>(10_000);

    private static volatile boolean running = true;

    private UdpReceiver() {}

    public static void start() {
        Thread t = new Thread(UdpReceiver::run, "udp-receiver");
        t.setDaemon(true);
        t.start();
    }

    public static void stop() {
        running = false;
    }

    public static BlockingQueue<UdpMessage> getQueue() {
        return queue;
    }

    private static void run() {
        try (DatagramSocket socket =
                     new DatagramSocket(null)) {

            socket.bind(new InetSocketAddress(PORT));
            socket.setReceiveBufferSize(1 << 20); // 1 MB buffer

            System.out.println("UDP receiver listening on port " + PORT);

            byte[] buffer = new byte[MAX_PACKET_SIZE];

            while (running) {
                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length);

                socket.receive(packet); // blocking

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
                        packet.getAddress().getHostAddress(),
                        packet.getPort(),
                        payload
                );

                queue.offer(msg); // non-blocking
            }
        } catch (Exception e) {
            System.err.println("UDP receiver error");
            e.printStackTrace();
        }
    }
}

