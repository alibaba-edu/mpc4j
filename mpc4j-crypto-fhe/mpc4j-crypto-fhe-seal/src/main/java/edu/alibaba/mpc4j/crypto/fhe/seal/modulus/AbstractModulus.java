package edu.alibaba.mpc4j.crypto.fhe.seal.modulus;

import edu.alibaba.mpc4j.crypto.fhe.seal.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Numth;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintCore;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represent an integer modulus of up to 61 bits. An instance of the Modulus class represents a non-negative integer
 * modulus up to 61 bits. In particular, the encryption parameter plain_modulus, and the primes in coeff_modulus, are
 * represented by instances of Modulus. The purpose of this class is to perform and store the pre-computation required
 * by Barrett reduction.
 * <p>
 * The implementation is from <code>Modulus</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/modulus.h#L33">
 * modulus.h
 * </a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2025/3/20
 */
public abstract class AbstractModulus {
    /**
     * modulus value, up to 61 bits, must be positive number
     */
    protected long value;
    /**
     * bit-count of value
     */
    private int bitCount;
    /**
     * uint64 count of value
     */
    private int uint64Count;
    /**
     * the Barrett ratio computed for the value of the current Modulus. The first two components of the Barrett ratio
     * are the floor of 2^128 / value, and the third component is the remainder.
     */
    private long[] constRatio;
    /**
     * Whether value is a prime number
     */
    private boolean isPrime;

    /**
     * Creates a Modulus instance. The value of the Modulus is set to zero by default.
     */
    public AbstractModulus() {
        setValue(0);
    }

    /**
     * Creates a Modulus instance. The value of the Modulus is set to the given value.
     *
     * @param value a given value.
     */
    public AbstractModulus(long value) {
        setValue(value);
    }

    /**
     * Creates a new Modulus by copying a given one.
     *
     * @param other the Modulus to copy from.
     */
    public AbstractModulus(AbstractModulus other) {
        this.value = other.value;
        this.bitCount = other.bitCount;
        this.uint64Count = other.uint64Count;
        this.isPrime = other.isPrime;
        this.constRatio = new long[3];
        System.arraycopy(other.constRatio, 0, constRatio, 0, 3);
    }

    /**
     * Sets the value of the Modulus.
     *
     * @param value the new integer modulus.
     */
    public void setValue(long value) {
        if (value == 0) {
            // zero settings
            bitCount = 0;
            uint64Count = 1;
            this.value = 0;
            constRatio = new long[]{0, 0, 0};
            isPrime = false;
        } else if (value >>> Constants.SEAL_MOD_BIT_COUNT_MAX != 0 || (value == 1)) {
            throw new IllegalArgumentException("value can be at most 61-bit and cannot be 1");
        } else {
            // All normal, compute const_ratio and set everything
            this.value = value;
            bitCount = UintCore.getSignificantBitCount(value);
            uint64Count = 1;
            constRatio = new long[3];
            // Compute Barrett ratios for 64-bit words (barrett_reduce_128)
            long[] numerator = new long[]{0, 0, 1};
            UintArithmetic.divideUint192Inplace(numerator, value, constRatio);
            constRatio[2] = numerator[0];
            // Set the primality flag
            isPrime = Numth.isPrime(value);
        }
    }

    /**
     * Returns the significant bit count of the value of the current Modulus.
     *
     * @return the significant bit count of the value of the current Modulus.
     */
    public int bitCount() {
        return bitCount;
    }

    /**
     * Returns the size (in 64-bit words) of the value of the current Modulus.
     *
     * @return the size (in 64-bit words) of the value of the current Modulus.
     */
    public int uint64Count() {
        return uint64Count;
    }

    /**
     * Returns the value of the current Modulus.
     *
     * @return the value of the current Modulus.
     */
    public long value() {
        return value;
    }

    /**
     * Returns the Barrett ratio computed for the value of the current Modulus.
     * The first two components of the Barrett ratio are the floor of 2^128 / value,
     * and the third component is the remainder.
     *
     * @return the Barrett ratio computed for the value of the current Modulus.
     */
    public long[] constRatio() {
        return constRatio;
    }

    /**
     * Returns whether the value of the current Modulus is zero.
     *
     * @return true if the value of the current Modulus is zero; false otherwise.
     */
    public boolean isZero() {
        return value == 0;
    }

    /**
     * Returns whether the value of the current Modulus is a prime number.
     *
     * @return true if the value of the current Modulus is a prime number; false otherwise.
     */
    public boolean isPrime() {
        return isPrime;
    }

    /**
     * Reduces a given unsigned integer modulo this modulus.
     *
     * @param input the unsigned integer to reduce.
     * @return input mod value.
     */
    public abstract long reduce(long input);

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(value)
            .toHashCode();
    }
}
