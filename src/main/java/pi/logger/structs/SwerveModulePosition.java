package pi.logger.structs;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.util.struct.StructSerializable;

/** Represents a swerve module position: distance and angle */
public final class SwerveModulePosition implements StructSerializable {
    public final double distance;
    public final Rotation2d angle;

    public SwerveModulePosition() {
        this(0.0, Rotation2d.fromDegrees(0.0));
    }

    public SwerveModulePosition(double distance, Rotation2d angle) {
        this.distance = distance;
        this.angle = angle;
    }

    /** Struct helper for NetworkTables and StructLogEntry */
    public static final SwerveModulePositionStruct struct = new SwerveModulePositionStruct();

    /** Create from a WPILib SwerveModulePosition-like object using reflection. */
    public static SwerveModulePosition fromWPILibObject(Object wp) {
        try {
            Class<?> cls = wp.getClass();
            double distance = 0.0;
            try {
                distance = ((Number) cls.getField("distanceMeters").get(wp)).doubleValue();
            } catch (NoSuchFieldException nsf) {
                try {
                    distance = ((Number) cls.getMethod("getDistanceMeters").invoke(wp)).doubleValue();
                } catch (NoSuchMethodException ns2) {
                    // Give up and leave as 0.0
                }
            }
            Rotation2d angle = null;
            try {
                angle = (Rotation2d) cls.getField("angle").get(wp);
            } catch (NoSuchFieldException nsf) {
                try {
                    angle = (Rotation2d) cls.getMethod("getAngle").invoke(wp);
                } catch (NoSuchMethodException ns2) {
                    angle = Rotation2d.fromDegrees(0.0);
                }
            }
            return new SwerveModulePosition(distance, angle);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid WPILib SwerveModulePosition object", e);
        }
    }

    /** Create a WPILib SwerveModulePosition-like object via reflection; returns null if unavailable. */
    public Object toWPILibObject() {
        try {
            Class<?> cls = Class.forName("edu.wpi.first.math.kinematics.SwerveModulePosition");
            try {
                return cls.getConstructor(double.class, Rotation2d.class).newInstance(distance, angle);
            } catch (NoSuchMethodException e) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("SwerveModulePosition(distance=%.6f, angle=%s)", distance, angle);
    }
}
