package pi.logger;

import edu.wpi.first.networktables.*;
import edu.wpi.first.networktables.NetworkTableEvent.Kind;
import java.util.EnumSet;

public final class MatchInfoListener {

    private static volatile int matchNumber = -1;
    private static volatile boolean enabled = false;

    private MatchInfoListener() {}

    public static void start() {
        NetworkTableInstance inst = NtClient.get();
        NetworkTable fms = inst.getTable("FMSInfo");
        NetworkTable ds  = inst.getTable("DriverStation");
        inst.addListener(
                fms.getEntry("MatchNumber"),
                EnumSet.of(Kind.kValueAll),
                event -> {
                    matchNumber = (int) event.valueData.value.getInteger();
                    System.out.println("Match number = " + matchNumber);
                }
        );
        inst.addListener(
                ds.getEntry("Enabled"),
                EnumSet.of(Kind.kValueAll),
                event -> {
                    enabled = event.valueData.value.getBoolean();
                    System.out.println("Robot enabled = " + enabled);
                }
        );
    }

    public static int getMatchNumber() {
        return matchNumber;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}

