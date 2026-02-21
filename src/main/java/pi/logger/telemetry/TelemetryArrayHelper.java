package pi.logger.telemetry;

public class TelemetryArrayHelper {
    public static boolean[] getBooleanArray(String payloadString) 
    {
        String[] parts = payloadString.split(";");
        boolean[] boolArray = new boolean[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            boolArray[i - 1] = Boolean.parseBoolean(parts[i]);
        }
        return boolArray;
    }
    public static float[] getFloatArray(String payloadString) 
    {
        String[] parts = payloadString.split(";");
        float[] floatArray = new float[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            floatArray[i - 1] = Float.parseFloat(parts[i]);
        }
        return floatArray;
    }
    public static long[] getIntArray(String payloadString) 
    {
        String[] parts = payloadString.split(";");
        long[] longArray = new long[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            longArray[i - 1] = Long.parseLong(parts[i]);
        }
        return longArray;
    }
    public static double[] getDoubleArray(String payloadString) 
    {
        String[] parts = payloadString.split(";");
        double[] doubleArray = new double[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            doubleArray[i - 1] = Double.parseDouble(parts[i]);
        }
        return doubleArray;
    }
}
