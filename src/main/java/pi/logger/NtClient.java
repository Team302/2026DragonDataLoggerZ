package pi.logger;

import edu.wpi.first.networktables.NetworkTableInstance;

public final class NtClient {

    private static final NetworkTableInstance inst =
            NetworkTableInstance.getDefault();

    private NtClient() {}

    public static void start(String roboRioIp) {
        inst.startClient4("pi-logger");
        inst.setServer(roboRioIp);
        System.out.println("NT client started, server=" + roboRioIp);

    }

    public static NetworkTableInstance get() {
        return inst;
    }
}

