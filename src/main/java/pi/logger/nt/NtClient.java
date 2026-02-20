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
package pi.logger.nt;

import edu.wpi.first.networktables.NetworkTableInstance;
import pi.logger.config.LoggerConfig;

public final class NtClient {

    private static final NetworkTableInstance inst =
            NetworkTableInstance.getDefault();
    private static final int DEFAULT_TEAM_NUMBER = 302;
    private static final String FALLBACK_SERVER = "localhost";
    private static final String DEFAULT_CLIENT_IDENTITY = "pi-logger";

    private NtClient() {}

    public static void start() {
        String clientIdentity = LoggerConfig.getString("nt.clientIdentity", DEFAULT_CLIENT_IDENTITY);
        inst.startClient4(clientIdentity);

        String resolvedServer = resolveServer();
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

    private static String resolveServer() {
        String override = LoggerConfig.getString("nt.serverOverride", "");
        return override.isBlank() ? null : override;
    }

    private static int resolveTeamNumber() {
        return LoggerConfig.getInt("nt.team", DEFAULT_TEAM_NUMBER, 1, Integer.MAX_VALUE);
    }
}

