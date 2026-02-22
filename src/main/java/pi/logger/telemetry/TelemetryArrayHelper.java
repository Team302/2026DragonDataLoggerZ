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
package pi.logger.telemetry;

public final class TelemetryArrayHelper {
    private TelemetryArrayHelper() {}
    public static boolean[] getBooleanArray(String payloadString)
    {
        String[] parts = payloadString.split(";");
        boolean[] boolArray = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) {
            boolArray[i] = Boolean.parseBoolean(parts[i]);
        }
        return boolArray;
    }
    public static float[] getFloatArray(String payloadString)
    {
        String[] parts = payloadString.split(";");
        float[] floatArray = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            floatArray[i] = Float.parseFloat(parts[i]);
        }
        return floatArray;
    }
    public static long[] getIntArray(String payloadString) 
    {
        String[] parts = payloadString.split(";");
        long[] longArray = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            longArray[i] = Long.parseLong(parts[i]);
        }
        return longArray;
    }
    public static double[] getDoubleArray(String payloadString) 
    {
        String[] parts = payloadString.split(";");
        double[] doubleArray = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            doubleArray[i] = Double.parseDouble(parts[i]);
        }
        return doubleArray;
    }
}
