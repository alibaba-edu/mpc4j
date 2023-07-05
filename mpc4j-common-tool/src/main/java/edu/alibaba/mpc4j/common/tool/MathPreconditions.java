/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package edu.alibaba.mpc4j.common.tool;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.math3.util.Precision;

import java.math.BigInteger;

/**
 * A collection of preconditions for math functions. The implementation is from:
 * <p>
 * https://github.com/google/guava/blob/master/guava/src/com/google/common/math/MathPreconditions.java
 * </p>
 * We need to copy the source code since it is originally package-private.
 *
 * @author Louis Wasserman
 * @date 2022/12/28
 */
public class MathPreconditions {

    private MathPreconditions() {
        // empty
    }

    /**
     * Check x > 0.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x <= 0.
     */
    @CanIgnoreReturnValue
    public static int checkPositive(String role, int x) {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Check x > 0.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x <= 0.
     */
    @CanIgnoreReturnValue
    public static long checkPositive(String role, long x) {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Check x > 0.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x <= 0.
     */
    @CanIgnoreReturnValue
    public static double checkPositive(String role, double x) {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Check x > 0.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x <= 0.
     */
    @CanIgnoreReturnValue
    public static BigInteger checkPositive(String role, BigInteger x) {
        if (x.signum() <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Check x >= 0.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x < 0.
     */
    @CanIgnoreReturnValue
    public static int checkNonNegative(String role, int x) {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Check x >= 0.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x < 0.
     */
    @CanIgnoreReturnValue
    public static long checkNonNegative(String role, long x) {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Check x >= 0.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x < 0.
     */
    @CanIgnoreReturnValue
    public static double checkNonNegative(String role, double x) {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Check x >= 0.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x < 0.
     */
    @CanIgnoreReturnValue
    public static BigInteger checkNonNegative(String role, BigInteger x) {
        if (x.signum() < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Check x == y.
     *
     * @param roleX the name of the value x.
     * @param roleY the name of the value y.
     * @param x     the value x.
     * @param y     the value y.
     * @throws IllegalArgumentException if x != y.
     */
    public static void checkEqual(String roleX, String roleY, int x, int y) {
        if (x != y) {
            throw new IllegalArgumentException(roleX + " (" + x + ") must be equal to " + roleY + " (" + y + ")");
        }
    }

    /**
     * Check x == y.
     *
     * @param roleX the name of the value x.
     * @param roleY the name of the value y.
     * @param x     the value x.
     * @param y     the value y.
     * @throws IllegalArgumentException if x != y.
     */
    public static void checkEqual(String roleX, String roleY, long x, long y) {
        if (x != y) {
            throw new IllegalArgumentException(roleX + " (" + x + ") must be equal to " + roleY + " (" + y + ")");
        }
    }

    /**
     * Check x == y.
     *
     * @param roleX     the name of the value x.
     * @param roleY     the name of the value y.
     * @param x         the value x.
     * @param y         the value y.
     * @param precision the precision, that is, x == y iff |x - y| <= precision.
     * @throws IllegalArgumentException if x != y.
     */
    public static void checkEqual(String roleX, String roleY, double x, double y, double precision) {
        if (!Precision.equals(x, y, precision)) {
            throw new IllegalArgumentException(roleX + " (" + x + ") must be equal to " + roleY + " (" + y + ") with precision (" + precision + ")");
        }
    }

    /**
     * Check x == y.
     *
     * @param roleX the name of the value x.
     * @param roleY the name of the value y.
     * @param x     the value x.
     * @param y     the value y.
     * @throws IllegalArgumentException if x != y.
     */
    public static void checkEqual(String roleX, String roleY, BigInteger x, BigInteger y) {
        if (!x.equals(y)) {
            throw new IllegalArgumentException(roleX + " (" + x + ") must be equal to " + roleY + " (" + y + ")");
        }
    }

    /**
     * Check x > min.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x <= min.
     */
    @CanIgnoreReturnValue
    public static int checkGreater(String role, int x, int min) {
        if (x <= min) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > " + min);
        }
        return x;
    }

    /**
     * Check x > min.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x <= min.
     */
    @CanIgnoreReturnValue
    public static long checkGreater(String role, long x, long min) {
        if (x <= min) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > " + min);
        }
        return x;
    }

    /**
     * Check x > min.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x <= min.
     */
    @CanIgnoreReturnValue
    public static double checkGreater(String role, double x, double min) {
        if (x <= min) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > " + min);
        }
        return x;
    }

    /**
     * Check x > min.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x <= min.
     */
    @CanIgnoreReturnValue
    public static BigInteger checkGreater(String role, BigInteger x, BigInteger min) {
        if (BigIntegerUtils.lessOrEqual(x, min)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > " + min);
        }
        return x;
    }

    /**
     * Check x >= min.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x < min.
     */
    @CanIgnoreReturnValue
    public static int checkGreaterOrEqual(String role, int x, int min) {
        if (x < min) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= " + min);
        }
        return x;
    }

    /**
     * Check x >= min.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x < min.
     */
    @CanIgnoreReturnValue
    public static long checkGreaterOrEqual(String role, long x, long min) {
        if (x < min) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= " + min);
        }
        return x;
    }

    /**
     * Check x >= min.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x < min.
     */
    @CanIgnoreReturnValue
    public static double checkGreaterOrEqual(String role, double x, double min) {
        if (x < min) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= " + min);
        }
        return x;
    }

    /**
     * Check x >= min.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x < min.
     */
    @CanIgnoreReturnValue
    public static BigInteger checkGreaterOrEqual(String role, BigInteger x, BigInteger min) {
        if (BigIntegerUtils.less(x, min)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= " + min);
        }
        return x;
    }

    /**
     * Check x < max.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if x >= max.
     */
    @CanIgnoreReturnValue
    public static int checkLess(String role, int x, int max) {
        if (x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be < " + max);
        }
        return x;
    }

    /**
     * Check x < max.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if x >= max.
     */
    @CanIgnoreReturnValue
    public static long checkLess(String role, long x, long max) {
        if (x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be < " + max);
        }
        return x;
    }

    /**
     * Check x < max.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if x >= max.
     */
    @CanIgnoreReturnValue
    public static double checkLess(String role, double x, double max) {
        if (x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be < " + max);
        }
        return x;
    }

    /**
     * Check x < max.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if x >= max.
     */
    @CanIgnoreReturnValue
    public static BigInteger checkLess(String role, BigInteger x, BigInteger max) {
        if (BigIntegerUtils.greaterOrEqual(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be < " + max);
        }
        return x;
    }

    /**
     * Check x <= max.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if x > max.
     */
    @CanIgnoreReturnValue
    public static int checkLessOrEqual(String role, int x, int max) {
        if (x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be <= " + max);
        }
        return x;
    }

    /**
     * Check x <= max.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if x > max.
     */
    @CanIgnoreReturnValue
    public static long checkLessOrEqual(String role, long x, long max) {
        if (x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be <= " + max);
        }
        return x;
    }

    /**
     * Check x <= max.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if x > max.
     */
    @CanIgnoreReturnValue
    public static double checkLessOrEqual(String role, double x, double max) {
        if (x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be <= " + max);
        }
        return x;
    }

    /**
     * Check x <= max.
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if x >= max.
     */
    @CanIgnoreReturnValue
    public static BigInteger checkLessOrEqual(String role, BigInteger x, BigInteger max) {
        if (BigIntegerUtils.greater(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be <= " + max);
        }
        return x;
    }

    /**
     * Check x ∈ (0, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ (0, max).
     */
    @CanIgnoreReturnValue
    public static int checkPositiveInRange(String role, int x, int max) {
        checkGreater("max", max, 1);
        if (x <= 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ (0, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ (0, max).
     */
    @CanIgnoreReturnValue
    public static long checkPositiveInRange(String role, long x, long max) {
        checkGreater("max", max, 1);
        if (x <= 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ (0, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ (0, max).
     */
    @CanIgnoreReturnValue
    public static double checkPositiveInRange(String role, double x, double max) {
        checkGreater("max", max, 0);
        if (x <= 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ (0, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ (0, max).
     */
    @CanIgnoreReturnValue
    public static BigInteger checkPositiveInRange(String role, BigInteger x, BigInteger max) {
        checkGreater("max", max, BigInteger.ONE);
        if (x.signum() <= 0 || BigIntegerUtils.greaterOrEqual(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ (0, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ (0, max].
     */
    @CanIgnoreReturnValue
    public static int checkPositiveInRangeClosed(String role, int x, int max) {
        checkGreater("max", max, 0);
        if (x <= 0 || x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ (0, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ (0, max].
     */
    @CanIgnoreReturnValue
    public static long checkPositiveInRangeClosed(String role, long x, long max) {
        checkGreater("max", max, 0);
        if (x <= 0 || x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ (0, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ (0, max].
     */
    @CanIgnoreReturnValue
    public static double checkPositiveInRangeClosed(String role, double x, double max) {
        checkGreater("max", max, 0);
        if (x <= 0 || x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ (0, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ (0, max].
     */
    @CanIgnoreReturnValue
    public static BigInteger checkPositiveInRangeClosed(String role, BigInteger x, BigInteger max) {
        checkGreater("max", max, BigInteger.ZERO);
        if (x.signum() <= 0 || BigIntegerUtils.greater(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static int checkNonNegativeInRange(String role, int x, int max) {
        checkPositive("max", max);
        if (x < 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static long checkNonNegativeInRange(String role, long x, long max) {
        checkPositive("max", max);
        if (x < 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static double checkNonNegativeInRange(String role, double x, double max) {
        checkPositive("max", max);
        if (x < 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static BigInteger checkNonNegativeInRange(String role, BigInteger x, BigInteger max) {
        checkPositive("max", max);
        if (x.signum() < 0 || BigIntegerUtils.greaterOrEqual(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max < 0 or x ∉ [0, max].
     */
    @CanIgnoreReturnValue
    public static int checkNonNegativeInRangeClosed(String role, int x, int max) {
        checkNonNegative("max", max);
        if (x < 0 || x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max < 0 or x ∉ [0, max].
     */
    @CanIgnoreReturnValue
    public static long checkNonNegativeInRangeClosed(String role, long x, long max) {
        checkNonNegative("max", max);
        if (x < 0 || x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max < 0 or x ∉ [0, max].
     */
    @CanIgnoreReturnValue
    public static double checkNonNegativeInRangeClosed(String role, double x, double max) {
        checkNonNegative("max", max);
        if (x < 0 || x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max < 0 or x ∉ [0, max].
     */
    @CanIgnoreReturnValue
    public static BigInteger checkNonNegativeInRangeClosed(String role, BigInteger x, BigInteger max) {
        checkNonNegative("max", max);
        if (x.signum() < 0 || BigIntegerUtils.greater(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ [min, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= min or x ∉ [min, max).
     */
    @CanIgnoreReturnValue
    public static int checkInRange(String role, int x, int min, int max) {
        checkGreater("max", max, min);
        if (x < min || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [" + min + ", " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [min, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= min or x ∉ [min, max).
     */
    @CanIgnoreReturnValue
    public static long checkInRange(String role, long x, long min, long max) {
        checkGreater("max", max, min);
        if (x < min || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [" + min + ", " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [min, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= min or x ∉ [min, max).
     */
    @CanIgnoreReturnValue
    public static double checkInRange(String role, double x, double min, double max) {
        checkGreater("max", max, min);
        if (x < min || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [" + min + ", " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [min, max).
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= min or x ∉ [min, max).
     */
    @CanIgnoreReturnValue
    public static BigInteger checkInRange(String role, BigInteger x, BigInteger min, BigInteger max) {
        checkGreater("max", max, min);
        if (BigIntegerUtils.less(x, min) || BigIntegerUtils.greaterOrEqual(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [" + min + ", " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [min, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max < min or x ∉ [min, max].
     */
    @CanIgnoreReturnValue
    public static int checkInRangeClosed(String role, int x, int min, int max) {
        checkGreaterOrEqual("max", max, min);
        if (x < min || x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [" + min + ", " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ [min, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max < min or x ∉ [min, max].
     */
    @CanIgnoreReturnValue
    public static long checkInRangeClosed(String role, long x, long min, long max) {
        checkGreaterOrEqual("max", max, min);
        if (x < min || x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [" + min + ", " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ [min, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max < min or x ∉ [min, max].
     */
    @CanIgnoreReturnValue
    public static double checkInRangeClosed(String role, double x, double min, double max) {
        checkGreaterOrEqual("max", max, min);
        if (x < min || x > max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [" + min + ", " + max + "]");
        }
        return x;
    }

    /**
     * Check x ∈ [min, max].
     *
     * @param role the name of the value x.
     * @param x    the value x.
     * @param min  the value min.
     * @param max  the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max < min or x ∉ [min, max].
     */
    @CanIgnoreReturnValue
    public static BigInteger checkInRangeClosed(String role, BigInteger x, BigInteger min, BigInteger max) {
        checkGreaterOrEqual("max", max, min);
        if (BigIntegerUtils.less(x, min) || BigIntegerUtils.greater(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [" + min + ", " + max + "]");
        }
        return x;
    }
}
