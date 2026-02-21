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
package pi.logger.csvparsers;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

/**
 * Utility for converting double arrays into FRC {@link Pose2d} objects.
 *
 * <p>The expected array format is {@code [x, y, rotation]} where x and y are in
 * meters and rotation is in radians by default (or degrees when specified).
 */
public final class Pose2dUtil {

    /** Minimum array length required: x, y, and rotation. */
    public static final int REQUIRED_LENGTH = 3;

    private Pose2dUtil() {}

    public static Pose2d fromString(String str) {
        if (str == null || str.isBlank()) {
            System.out.println("Input string is null or blank, cannot parse Pose2d");
            throw new IllegalArgumentException("Input string cannot be null or blank");
        }
        String[] parts = str.split(";", 3);
        if (parts.length < REQUIRED_LENGTH) {
            System.out.println("Input string does not have enough parts, cannot parse Pose2d: " + str);
            throw new IllegalArgumentException(
                "Input string must have at least " + REQUIRED_LENGTH + " parts separated by ';' [x;y;rotation]"
            );
        }
        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double rotation = Double.parseDouble(parts[2].trim());
            Pose2d pose = new Pose2d(new Translation2d(x, y), Rotation2d.fromRadians(rotation));
            return pose;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format in input string, cannot parse Pose2d: " + str);
            throw new IllegalArgumentException("Invalid number format in input string: " + str, e);
        }
    }

}
