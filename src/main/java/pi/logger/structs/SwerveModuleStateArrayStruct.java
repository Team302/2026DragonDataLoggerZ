package pi.logger.structs;

import java.nio.ByteBuffer;

import edu.wpi.first.util.struct.Struct;

public class SwerveModuleStateArrayStruct implements Struct<SwerveModuleState[]> {
    private static final int MODULE_COUNT = 4;

    @Override
    public Class<SwerveModuleState[]> getTypeClass() {
        return SwerveModuleState[].class;
    }

    @Override
    public String getTypeName() {
        return "SwerveModuleState[]";
    }

    @Override
    public int getSize() {
        return MODULE_COUNT * SwerveModuleState.struct.getSize();
    }

    @Override
    public String getSchema() {
        return "SwerveModuleState[" + MODULE_COUNT + "] modules";
    }

    @Override
    public Struct<?>[] getNested() {
        return new Struct<?>[] { SwerveModuleState.struct };
    }

    @Override
    public SwerveModuleState[] unpack(ByteBuffer bb) {
        SwerveModuleState[] output = new SwerveModuleState[MODULE_COUNT];
        for (int i = 0; i < MODULE_COUNT; i++) {
            output[i] = SwerveModuleState.struct.unpack(bb);
        }
        return output;
    }

    @Override
    public void pack(ByteBuffer bb, SwerveModuleState[] value) {
        SwerveModuleState[] modules = value;
        if (modules == null || modules.length != MODULE_COUNT) {
            modules = new SwerveModuleState[MODULE_COUNT];
            for (int i = 0; i < MODULE_COUNT; i++) {
                modules[i] = new SwerveModuleState();
            }
        }
        for (SwerveModuleState module : modules) {
            SwerveModuleState.struct.pack(bb, module);
        }
    }

    @Override
    public boolean isImmutable() {
        return false;
    }
}
