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
package pi.logger.config;

import java.io.InputStream;
import java.util.Properties;

public final class LoggerConfig {

    private static final String CONFIG_RESOURCE_PATH = "config/logger.properties";
    private static final Properties PROPERTIES = loadProperties();

    private LoggerConfig() {}

    public static int getInt(String key, int defaultValue, int minAllowed, int maxAllowed) {
        String raw = PROPERTIES.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < minAllowed || parsed > maxAllowed) {
                System.err.println("Invalid value for " + key + " (" + parsed + "). Using default " + defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException e) {
            System.err.println("Invalid number for " + key + " ('" + raw + "'). Using default " + defaultValue);
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue, long minAllowed) {
        String raw = PROPERTIES.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            long parsed = Long.parseLong(raw.trim());
            if (parsed < minAllowed) {
                System.err.println("Invalid value for " + key + " (" + parsed + "). Using default " + defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException e) {
            System.err.println("Invalid number for " + key + " ('" + raw + "'). Using default " + defaultValue);
            return defaultValue;
        }
    }

    public static String getString(String key, String defaultValue) {
        String raw = PROPERTIES.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        return raw.trim();
    }

    public static double getDouble(String key, double defaultValue, double minAllowed) {
        String raw = PROPERTIES.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            double parsed = Double.parseDouble(raw.trim());
            if (parsed < minAllowed) {
                System.err.println("Invalid value for " + key + " (" + parsed + "). Using default " + defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException e) {
            System.err.println("Invalid number for " + key + " ('" + raw + "'). Using default " + defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String raw = PROPERTIES.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        String normalized = raw.trim().toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
            return false;
        }

        System.err.println("Invalid boolean for " + key + " ('" + raw + "'). Using default " + defaultValue);
        return defaultValue;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = LoggerConfig.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE_PATH)) {
            if (in != null) {
                properties.load(in);
            } else {
                System.err.println("Config not found at " + CONFIG_RESOURCE_PATH + ". Using defaults.");
            }
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }
        return properties;
    }
}
