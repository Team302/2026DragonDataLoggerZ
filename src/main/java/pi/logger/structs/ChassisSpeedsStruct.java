package pi.logger.structs;

import edu.wpi.first.util.struct.Struct;
import java.nio.ByteBuffer;

public class ChassisSpeedsStruct implements Struct<ChassisSpeeds> {
    @Override
    public Class<ChassisSpeeds> getTypeClass() {
        return ChassisSpeeds.class;
    }

    @Override
    public String getTypeName() {
        return "ChassisSpeeds";
    }

    @Override
    public int getSize() {
        return Double.BYTES * 3;
    }

    @Override
    public String getSchema() {
        return "double vx;double vy;double omega";
    }

    @Override
    public Struct<?>[] getNested() {
        return new Struct<?>[0];
    }

    @Override
    public ChassisSpeeds unpack(ByteBuffer bb) {
        double vx = bb.getDouble();
        double vy = bb.getDouble();
        double omega = bb.getDouble();
        return new ChassisSpeeds(vx, vy, omega);
    }

    @Override
    public void pack(ByteBuffer bb, ChassisSpeeds value) {
        bb.putDouble(value.vx);
        bb.putDouble(value.vy);
        bb.putDouble(value.omega);
    }

    @Override
    public boolean isImmutable() {
        return true;
    }
}
