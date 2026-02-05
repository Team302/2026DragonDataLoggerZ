package pi.logger.structs;

import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.math.geometry.Rotation2d;
import java.nio.ByteBuffer;

public class SwerveModulePositionStruct implements Struct<SwerveModulePosition> {
    @Override
    public Class<SwerveModulePosition> getTypeClass() {
        return SwerveModulePosition.class;
    }

    @Override
    public String getTypeName() {
        return "SwerveModulePosition";
    }

    @Override
    public int getSize() {
        return Double.BYTES + Rotation2d.struct.getSize();
    }

    @Override
    public String getSchema() {
        return "double distance;Rotation2d angle";
    }

    @Override
    public Struct<?>[] getNested() {
        return new Struct<?>[] { Rotation2d.struct };
    }

    @Override
    public SwerveModulePosition unpack(ByteBuffer bb) {
        double distance = bb.getDouble();
        Rotation2d angle = Rotation2d.struct.unpack(bb);
        return new SwerveModulePosition(distance, angle);
    }

    @Override
    public void pack(ByteBuffer bb, SwerveModulePosition value) {
        bb.putDouble(value.distance);
        Rotation2d.struct.pack(bb, value.angle);
    }

    @Override
    public boolean isImmutable() {
        return true;
    }
}
