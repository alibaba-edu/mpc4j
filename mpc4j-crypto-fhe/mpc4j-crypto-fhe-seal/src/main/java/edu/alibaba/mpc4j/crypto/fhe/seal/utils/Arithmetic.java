package edu.alibaba.mpc4j.crypto.fhe.seal.utils;

import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.util.FastMath;

/**
 * Provides an interface to all necessary arithmetic of the number structure that specializes a DWTHandler.
 * The original implementation defines <code>Arithmetic</code> using
 * <code>template <typename ValueType, typename RootType, typename ScalarType></code>.
 * However, as shown in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/ckks.h#L108">ckks.h</a>,
 * it is instanced as {@code <std::complex<double>, std::complex<double>, double>}.
 * <ul>
 * <li><code>ValueType</code>: <code>double[2]</code></li>
 * <li><code>RootType</code>: <code>double[2]</code></li>
 * <li><code>ScalarType</code>: <code>double</code></li>
 * </ul>
 *
 * <p>
 * The implementation comes from <code>Arithmetic</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/dwthandler.h#L23">dwthandler.h</a>.
 *
 * @author Weiran Liu
 * @date 2025/2/14
 */
public class Arithmetic {
    /**
     * private constructor.
     */
    private Arithmetic() {
        // empty
    }

    /**
     * Create a zero complex number.
     *
     * @return a zero complex number.
     */
    public static double[] createZero() {
        return new double[2];
    }

    /**
     * Create a complex number given only the real part.
     *
     * @param real real part.
     * @return the complex number.
     */
    public static double[] create(double real) {
        return create(real, 0.0);
    }

    /**
     * Sets the complex number as the real part.
     *
     * @param real real part.
     */
    public static void set(double[] complex, double real) {
        set(complex, real, 0.0);
    }

    /**
     * Create a complex number given the real and imaginary parts.
     *
     * @param real      Real part.
     * @param imaginary Imaginary part.
     * @return the complex number.
     */
    public static double[] create(double real, double imaginary) {
        return new double[]{real, imaginary};
    }

    /**
     * Sets the complex number as the real and imaginary parts.
     *
     * @param real      real part.
     * @param imaginary imaginary part.
     */
    public static void set(double[] complex, double real, double imaginary) {
        assert isValid(complex);
        complex[0] = real;
        complex[1] = imaginary;
    }

    /**
     * Sets the complex number as 0.
     *
     * @param complex the complex number.
     */
    public static void setZero(double[] complex) {
        assert isValid(complex);
        complex[0] = 0.0;
        complex[1] = 0.0;
    }

    /**
     * Sets the complex number as the given value.
     *
     * @param complex complex number.
     * @param value   given value.
     */
    public static void set(double[] complex, double[] value) {
        assert isValid(complex);
        assert isValid(value);
        complex[0] = value[0];
        complex[1] = value[1];
    }

    /**
     * Returns whether data is a valid complex number. A valid complex number must not be null and have two parts
     * (real part and  imaginary part).
     *
     * @param data data.
     * @return true if this data is a valid complex number; false otherwise.
     */
    public static boolean isValid(double[] data) {
        return data != null && data.length == 2;
    }

    /**
     * Returns whether the complex number is equal to NaN.
     *
     * @param complex the complex number.
     * @return true if this complex number is NaN; false otherwise.
     */
    public static boolean isNaN(double[] complex) {
        assert isValid(complex);
        return Double.isNaN(complex[0]) || Double.isNaN(complex[1]);
    }

    /**
     * Returns whether the complex number is infinite.
     *
     * @param complex the complex number.
     * @return true if this complex number is infinite; false otherwise.
     */
    public static boolean isInfinite(double[] complex) {
        assert isValid(complex);
        return !isNaN(complex) && (Double.isInfinite(complex[0]) || Double.isInfinite(complex[1]));
    }

    /**
     * Return the absolute value of the given complex number.
     * Returns {@code NaN} if either real or imaginary part is {@code NaN}
     * and {@code Double.POSITIVE_INFINITY} if neither part is {@code NaN},
     * but at least one part is infinite.
     *
     * @return the absolute value.
     */
    public static double abs(double[] complex) {
        assert isValid(complex);
        if (isNaN(complex)) {
            return Double.NaN;
        }
        if (isInfinite(complex)) {
            return Double.POSITIVE_INFINITY;
        }
        double real = complex[0];
        double imaginary = complex[1];
        if (FastMath.abs(real) < FastMath.abs(imaginary)) {
            if (imaginary == 0.0) {
                return FastMath.abs(real);
            }
            double q = real / imaginary;
            return FastMath.abs(imaginary) * FastMath.sqrt(1 + q * q);
        } else {
            if (real == 0.0) {
                return FastMath.abs(imaginary);
            }
            double q = imaginary / real;
            return FastMath.abs(real) * FastMath.sqrt(1 + q * q);
        }
    }

    /**
     * Sets {@code result} whose value is {@code (num1 + num2)}. Uses the definitional formula
     * <p>
     * {@code (a + bi) + (c + di) = (a + c) + (b + d)i}
     * </p>
     * If either {@code num1} or {@code num2} has a {@code NaN} value in either part, NaN is returned; otherwise
     * {@code Infinite} and {@code NaN} values are returned in the parts of the result according to the rules
     * for {@link Double} arithmetic.
     *
     * @param result result.
     * @param num1   num1.
     * @param num2   num2.
     */
    public static void add(double[] result, double[] num1, double[] num2) throws NullArgumentException {
        assert isValid(result);
        assert isValid(num1);
        assert isValid(num2);
        if (isNaN(num1) || isNaN(num2)) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
        } else {
            result[0] = num1[0] + num2[0];
            result[1] = num1[1] + num2[1];
        }
    }

    /**
     * Sets {@code result} whose value is {@code (num1 + num2)}. Uses the definitional formula
     * <p>
     * {@code (a + bi) + (c) = (a + c) + bi}
     * </p>
     * If either {@code num1} or {@code num2} has a {@code NaN} value in either part, NaN is returned; otherwise
     * {@code Infinite} and {@code NaN} values are returned in the parts of the result according to the rules
     * for {@link Double} arithmetic.
     *
     * @param result result.
     * @param num1   num1.
     * @param num2   num2.
     */
    public static void add(double[] result, double[] num1, double num2) throws NullArgumentException {
        assert isValid(result);
        assert isValid(num1);
        if (isNaN(num1) || Double.isNaN(num2)) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
        } else {
            result[0] = num1[0] + num2;
        }
    }

    /**
     * Sets {@code result} whose value is {@code (result[0] + real)}. Uses the definitional formula.
     * <p>
     * {@code (a + bi) + c = (a + c) + bi}
     * </p>
     *
     * @param result result.
     * @param real  real.
     */
    public static void addi(double[] result, double real) {
        assert isValid(result);
        if (isNaN(result) || Double.isNaN(real)) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
        } else {
            result[0] += real;
        }
    }

    /**
     * Sets {@code result} whose value is {@code (result + num)}. Uses the definitional formula.
     * <p>
     * {@code (a + bi) + (c + di) = (a + c) + (b + d)i}
     * </p>
     *
     * @param result result.
     * @param num  num..
     */
    public static void addi(double[] result, double[] num) {
        assert isValid(result);
        if (isNaN(result) || isNaN(num)) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
        } else {
            result[0] += num[0];
            result[1] += num[1];
        }
    }

    /**
     * Sets {@code result} whose value is {@code (num1 - num2)}. Uses the definitional formula
     * <p>
     * {@code (a + bi) - (c + di) = (a-c) + (b-d)i}
     * </p>
     * If either {@code num1} or {@code num2} has a {@code NaN]} value in either part, NaN is returned; otherwise
     * {@code Infinite} and {@code NaN} values are returned in the parts of the result according to the rules for
     * {@link Double} arithmetic.
     *
     * @param result result.
     * @param num1   num1.
     * @param num2   num2.
     */
    public static void sub(double[] result, double[] num1, double[] num2) throws NullArgumentException {
        assert isValid(result);
        assert isValid(num1);
        assert isValid(num2);
        if (isNaN(num1) || isNaN(num2)) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
        } else {
            result[0] = num1[0] - num2[0];
            result[1] = num1[1] - num2[1];
        }
    }

    /**
     * Sets {@code result} whose value is {@code (result[0] - real)}. Uses the definitional formula.
     * <p>
     * {@code (a + bi) - c = (a - c) + bi}
     * </p>
     *
     * @param result result.
     * @param real  real.
     */
    public static void subi(double[] result, double real) {
        assert isValid(result);
        if (isNaN(result) || Double.isNaN(real)) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
        } else {
            result[0] -= real;
        }
    }

    /**
     * Sets {@code result} whose value is {@code num1 * num2}. Implements preliminary checks for {@code NaN} and
     * infinity followed by the definitional formula:
     * <p>
     * {@code (a + bi)(c + di) = (ac - bd) + (ad + bc)i}
     * </p>
     * Returns NaN if either {@code num1} or {@code num2} has one or more {@code NaN} parts.
     * <p>
     * Returns INF if neither {@code num1} nor {@code num2} has one or more {@code NaN} parts and if either {@code num1}
     * or {@code num2} has one or more infinite parts (same result is returned regardless of the sign of the components).
     * </p><p>
     * Returns finite values in components of the result per the definitional formula in all remaining cases.</p>
     *
     * @param result result.
     * @param num1   num1.
     * @param num2   num2.
     */
    public static void mul(double[] result, double[] num1, double[] num2) throws NullArgumentException {
        assert isValid(result);
        assert isValid(num1);
        assert isValid(num2);
        if (isNaN(num1) || isNaN(num2)) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
        } else if (Double.isInfinite(num1[0]) || Double.isInfinite(num1[1]) ||
            Double.isInfinite(num2[0]) || Double.isInfinite(num2[1])) {
            // we don't use isInfinite() to avoid testing for NaN again
            result[0] = Double.POSITIVE_INFINITY;
            result[1] = Double.POSITIVE_INFINITY;
        } else {
            result[0] = num1[0] * num2[0] - num1[1] * num2[1];
            result[1] = num1[0] * num2[1] + num1[1] * num2[0];
        }
    }

    /**
     * Sets {@code result} whose value is {@code num1 * num2}. Implements preliminary checks for {@code NaN} and
     * infinity followed by the definitional formula:
     * <p>
     * {@code (a + bi)(c) = (ac) + (bc)i}
     * </p>
     * Returns NaN if either {@code num1} or {@code num2} has one or more {@code NaN} parts.
     * <p>
     * Returns INF if neither {@code num1} nor {@code num2} has one or more {@code NaN} parts and if either {@code num1}
     * or {@code num2} has one or more infinite parts (same result is returned regardless of the sign of the components).
     * </p><p>
     * Returns finite values in components of the result per the definitional formula in all remaining cases.</p>
     *
     * @param result result.
     * @param num1   num1.
     * @param num2   num2.
     */
    public static void mul(double[] result, double[] num1, double num2) throws NullArgumentException {
        assert isValid(result);
        assert isValid(num1);
        if (isNaN(num1) || Double.isNaN(num2)) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
        } else if (Double.isInfinite(num1[0]) || Double.isInfinite(num1[1]) || Double.isInfinite(num2)) {
            // we don't use isInfinite() to avoid testing for NaN again
            result[0] = Double.POSITIVE_INFINITY;
            result[1] = Double.POSITIVE_INFINITY;
        } else {
            result[0] = num1[0] * num2;
            result[1] = num1[1] * num2;
        }
    }

    /**
     * Computes a {@code Complex} and in-place sets num1 as {@code num1 * num2}. Implements preliminary checks for
     * {@code NaN} and infinity followed by the definitional formula:
     * <p>
     * {@code (a + bi)(c + di) = (ac - bd) + (ad + bc)i}
     * </p>
     * Returns NaN if either {@code num1} or {@code num2} has one or more {@code NaN} parts.
     * <p>
     * Returns INF if neither {@code num1} nor {@code num2} has one or more {@code NaN} parts and if either {@code num1}
     * or {@code num2} has one or more infinite parts (same result is returned regardless of the sign of the components).
     * </p><p>
     * Returns finite values in components of the result per the definitional formula in all remaining cases.</p>
     *
     * @param num1 num1.
     * @param num2 num2.
     */
    public static void muli(double[] num1, double[] num2) throws NullArgumentException {
        assert isValid(num1);
        assert isValid(num2);
        if (isNaN(num1) || isNaN(num2)) {
            num1[0] = Double.NaN;
            num1[1] = Double.NaN;
        } else if (Double.isInfinite(num1[0]) || Double.isInfinite(num1[1]) ||
            Double.isInfinite(num2[0]) || Double.isInfinite(num2[1])) {
            // we don't use isInfinite() to avoid testing for NaN again
            num1[0] = Double.POSITIVE_INFINITY;
            num1[1] = Double.POSITIVE_INFINITY;
        } else {
            double real = num1[0] * num2[0] - num1[1] * num2[1];
            double imaginary = num1[0] * num2[1] + num1[1] * num2[0];
            num1[0] = real;
            num1[1] = imaginary;
        }
    }

    /**
     * Computes and in-place sets z as conj(z). If z = x + iy, then conj(z) = x - iy.
     *
     * @param z z.
     */
    public static void conji(double[] z) {
        assert isValid(z);
        z[1] = -z[1];
    }

    /**
     * Computes and in-place sets z as -z. If z = x + iy, then -z = -x - iy.
     *
     * @param z z.
     */
    public static void negi(double[] z) {
        assert isValid(z);
        z[0] = -z[0];
        z[1] = -z[1];
    }

    /**
     * Computes and in-place sets z = mirror(z). If z = x + iy, then mirror(z) = y + ix.
     *
     * @param z z.
     */
    public static void mirrori(double[] z) {
        double real = z[0];
        z[0] = z[1];
        z[1] = real;
    }

    /**
     * Gets the real part of the complex number.
     *
     * @param z z.
     * @return the real part.
     */
    public static double real(double[] z) {
        assert isValid(z);
        return z[0];
    }

    /**
     * Gets the imaginary part of the complex number.
     *
     * @param z z.
     * @return the imaginary part.
     */
    public static double imag(double[] z) {
        assert isValid(z);
        return z[1];
    }
}
