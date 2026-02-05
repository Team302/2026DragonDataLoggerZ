package pi.logger.structs;

import java.nio.ByteBuffer;

import edu.wpi.first.util.struct.Struct;

public class SwerveModulePositionArrayStruct implements Struct<SwerveModulePosition[]> {
    private static final int MODULE_COUNT = 4;

    @Override
    public Class<SwerveModulePosition[]> getTypeClass() {
        return SwerveModulePosition[].class;
    }

    @Override
    public String getTypeName() {
        return "SwerveModulePosition[]";
    }

    @Override
    public int getSize() {
        return MODULE_COUNT * SwerveModulePosition.struct.getSize();
    }

    @Override
    public String getSchema() {
        return "SwerveModulePosition[" + MODULE_COUNT + "] modules";
    }

    @Override
    public Struct<?>[] getNested() {
        return new Struct<?>[] { SwerveModulePosition.struct };
    }

    @Override
    public SwerveModulePosition[] unpack(ByteBuffer bb) {
        SwerveModulePosition[] states = new SwerveModulePosition[MODULE_COUNT];
        for (int i = 0; i < MODULE_COUNT; i++) {
            states[i] = SwerveModulePosition.struct.unpack(bb);
        }
        return states;
    }

    @Override
    public void pack(ByteBuffer bb, SwerveModulePosition[] value) {
        SwerveModulePosition[] modules = value;
        if (modules == null || modules.length != MODULE_COUNT) {
            modules = new SwerveModulePosition[MODULE_COUNT];
            for (int i = 0; i < MODULE_COUNT; i++) {
                modules[i] = new SwerveModulePosition();
            }
        }
        for (SwerveModulePosition module : modules) {
            SwerveModulePosition.struct.pack(bb, module);
        }
    }

    @Override
    public boolean isImmutable() {
        return false;
    }
}
