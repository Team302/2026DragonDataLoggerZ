package pi.logger.structs;

import edu.wpi.first.util.struct.StructSerializable;

/** Simple struct representing chassis speeds (vx, vy, omega) */
public final class ChassisSpeeds implements StructSerializable {
    public final double vx;
    public final double vy;
    public final double omega;

    public ChassisSpeeds() {
        this(0.0, 0.0, 0.0);
    }

    public ChassisSpeeds(double vx, double vy, double omega) {
        this.vx = vx;
        this.vy = vy;
        this.omega = omega;
    }

    /** Struct helper for NetworkTables and StructLogEntry */
    public static final ChassisSpeedsStruct struct = new ChassisSpeedsStruct();

    /**
     * Convert from a WPILib ChassisSpeeds instance using reflection so this
     * class doesn't require a compile-time dependency on WPILib units classes.
     */
    public static ChassisSpeeds fromWPILibObject(Object cs) {
        try {
            Class<?> cls = cs.getClass();
            double vx = ((Number) cls.getField("vxMetersPerSecond").get(cs)).doubleValue();
            double vy = ((Number) cls.getField("vyMetersPerSecond").get(cs)).doubleValue();
            double omega = ((Number) cls.getField("omegaRadiansPerSecond").get(cs)).doubleValue();
            return new ChassisSpeeds(vx, vy, omega);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid WPILib ChassisSpeeds object", e);
        }
    }

    /**
     * Create a WPILib ChassisSpeeds instance via reflection. Returns null if
     * WPILib types are not available at runtime.
     */
    public Object toWPILibObject() {
        try {
            Class<?> cls = Class.forName("edu.wpi.first.math.kinematics.ChassisSpeeds");
            return cls.getConstructor(double.class, double.class, double.class).newInstance(vx, vy, omega);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("ChassisSpeeds(vx=%.6f, vy=%.6f, omega=%.6f)", vx, vy, omega);
    }
}
