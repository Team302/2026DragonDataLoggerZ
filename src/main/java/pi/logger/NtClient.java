package pi.logger;

import edu.wpi.first.networktables.NetworkTableInstance;

public final class NtClient {

    private static final NetworkTableInstance inst =
            NetworkTableInstance.getDefault();
    private static final int DEFAULT_TEAM_NUMBER = 302;
    private static final String FALLBACK_SERVER = "localhost";

    private NtClient() {}

    public static void start(String serverOverride) {
        inst.startClient4("pi-logger");

        String resolvedServer = resolveServer(serverOverride);
        if (resolvedServer != null) {
            inst.setServer(resolvedServer);
            System.out.println("NT client started, server=" + resolvedServer);
            return;
        }

        int teamNumber = resolveTeamNumber();
        if (teamNumber > 0) {
            inst.setServerTeam(teamNumber);
            System.out.println("NT client started, team=" + teamNumber);
            return;
        }

        inst.setServer(FALLBACK_SERVER);
        System.out.println("NT client started, fallback server=" + FALLBACK_SERVER);
    }

    public static NetworkTableInstance get() {
        return inst;
    }

    private static String resolveServer(String override) {
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        String envServer = System.getenv("NT_SERVER");
        if (envServer != null && !envServer.isBlank()) {
            return envServer.trim();
        }
        String propServer = System.getProperty("nt.server");
        if (propServer != null && !propServer.isBlank()) {
            return propServer.trim();
        }
        return null;
    }

    private static int resolveTeamNumber() {
        Integer team = parseTeamNumber(System.getenv("NT_TEAM"));
        if (team == null) {
            team = parseTeamNumber(System.getProperty("nt.team"));
        }
        if (team != null) {
            return team;
        }
        return DEFAULT_TEAM_NUMBER;
    }

    private static Integer parseTeamNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            System.err.println("Invalid team number provided: " + value);
            return null;
        }
    }
}

