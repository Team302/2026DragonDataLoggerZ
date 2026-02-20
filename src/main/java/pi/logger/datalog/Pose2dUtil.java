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
package pi.logger.datalog;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;

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

    /**
     * Convert a double array to a {@link Pose2d}. Rotation is treated as radians.
     *
     * @param array array containing {@code [x, y, rotationRadians]}
     * @return the corresponding {@link Pose2d}
     * @throws IllegalArgumentException if the array is {@code null} or has fewer than 3 elements
     */
    public static Pose2d fromArray(double[] array) {
        return fromArray(array, false);
    }

    /**
     * Convert a double array to a {@link Pose2d}.
     *
     * @param array        array containing {@code [x, y, rotation]}
     * @param rotInDegrees {@code true} if the rotation value is in degrees; {@code false} for radians
     * @return the corresponding {@link Pose2d}
     * @throws IllegalArgumentException if the array is {@code null} or has fewer than 3 elements
     */
    public static Pose2d fromArray(double[] array, boolean rotInDegrees) {
        if (array == null || array.length < REQUIRED_LENGTH) {
            throw new IllegalArgumentException(
                "Pose2d array must have at least " + REQUIRED_LENGTH + " elements [x, y, rotation]"
            );
        }
        double x = array[0];
        double y = array[1];
        Rotation2d rotation = rotInDegrees
            ? Rotation2d.fromDegrees(array[2])
            : Rotation2d.fromRadians(array[2]);
        return new Pose2d(x, y, rotation);
    }
}
