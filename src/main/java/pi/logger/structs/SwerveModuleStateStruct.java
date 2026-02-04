package pi.logger.structs;

import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.math.geometry.Rotation2d;
import java.nio.ByteBuffer;

public class SwerveModuleStateStruct implements Struct<SwerveModuleState> {
    @Override
    public Class<SwerveModuleState> getTypeClass() {
        return SwerveModuleState.class;
    }

    @Override
    public String getTypeName() {
        return "SwerveModuleState";
    }

    @Override
    public int getSize() {
        return Double.BYTES + Rotation2d.struct.getSize();
    }

    @Override
    public String getSchema() {
        return "double speed;Rotation2d angle";
    }

    @Override
    public Struct<?>[] getNested() {
        return new Struct<?>[] { Rotation2d.struct };
    }

    @Override
    public SwerveModuleState unpack(ByteBuffer bb) {
        double speed = bb.getDouble();
        Rotation2d angle = Rotation2d.struct.unpack(bb);
        return new SwerveModuleState(speed, angle);
    }

    @Override
    public void pack(ByteBuffer bb, SwerveModuleState value) {
        bb.putDouble(value.speed);
        Rotation2d.struct.pack(bb, value.angle);
    }

    @Override
    public boolean isImmutable() {
        return true;
    }
}
