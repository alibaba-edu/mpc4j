package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * This struct contains an operand and a precomputed quotient: (2^64 * operand) / modulus, for a specific modulus.
 * When passed to multiply_uint_mod, a faster variant of Barrett reduction will be performed.
 * <p>Operand must be less than modulus.</p>
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/uintarithsmallmod.h#L255
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/10
 */
public class MultiplyUintModOperand {
    /**
     * the operand
     */
    public long operand;
    /**
     * the quotient, i.e., (2^64 * operand) / modulus
     */
    public long quotient;

    /**
     * Creates an uint64 mod multiplication operand.
     */
    public MultiplyUintModOperand() {
        operand = 0;
        quotient = 0;
    }

    /**
     * Sets the quotient as (2^64 * operand) / modulus.
     *
     * @param modulus a modulus.
     */
    public void setQuotient(Modulus modulus) {
        assert operand < modulus.value();
        long[] wideQuotient = new long[2];
        // 2^64 * operand
        long[] wideCoeff = new long[]{0, operand};
        // (2^64 * operand) / modulus
        UintArithmetic.divideUint128Inplace(wideCoeff, modulus.value(), wideQuotient);
        quotient = wideQuotient[0];
    }

    /**
     * Sets the operand and computes (2^64 * operand) / modulus.
     *
     * @param newOperand a new operand, a 64-bit value.
     * @param modulus    a modulus.
     */
    public void set(long newOperand, Modulus modulus) {
        assert newOperand < modulus.value();
        operand = newOperand;
        setQuotient(modulus);
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }
}
