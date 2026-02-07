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
