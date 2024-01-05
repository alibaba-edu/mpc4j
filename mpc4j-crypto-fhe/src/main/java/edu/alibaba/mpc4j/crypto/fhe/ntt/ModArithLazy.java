package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.MultiplyUintModOperand;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * lazy modulo arithmetic operations. Lazy means the modulo operation returns the result in [0, 2 * modulus - 1) rather
 * than [0, modulus) and all operations are without valid checking. This is only used in NTT computation.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/ntt.h#L21
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/27
 */
class ModArithLazy {
    /**
     * modulus
     */
    private final Modulus modulus;
    /**
     * 2 * modulus
     */
    private final long twoTimesModulus;

    /**
     * Creates a lazy modulo arithmetic operation.
     *
     * @param modulus modulus.
     */
    public ModArithLazy(Modulus modulus) {
        this.modulus = modulus;
        twoTimesModulus = modulus.value() << 1;
    }

    /**
     * Computes a + b mod modulus, where the result is in [0, 2 * modulus - 1).
     *
     * @param a a.
     * @param b b.
     * @return a + b mod modulus.
     */
    public long add(long a, long b) {
        return a + b;
    }

    /**
     * Computes a - b mod modulus, where the result is in [0, 2 * modulus - 1).
     *
     * @param a a.
     * @param b b.
     * @return a - b mod modulus.
     */
    public long sub(long a, long b) {
        return a + twoTimesModulus - b;
    }

    /**
     * Computes a * r mod modulus, where r is a root, and the result is in [0, 2 * modulus - 1).
     *
     * @param a a.
     * @param r the root r, with pre-computed values for barrett reduction.
     * @return a * r mod modulus.
     */
    public long mulRoot(long a, MultiplyUintModOperand r) {
        return UintArithmeticSmallMod.multiplyUintModLazy(a, r, modulus);
    }

    /**
     * Computes a * s mod modulus, where s is a scalar, and the result is in [0, 2 * modulus - 1).
     *
     * @param a a.
     * @param s the scalar s, with pre-computed values for barrett reduction.
     * @return a * s mod modulus.
     */
    public long mulScalar(long a, MultiplyUintModOperand s) {
        return UintArithmeticSmallMod.multiplyUintModLazy(a, s, modulus);
    }

    /**
     * Computes r * s mod modulus, where r is a root, s is a scalar, with the result with pre-computed values for
     * barrett reduction.
     *
     * @param r the root r, with pre-computed values for barrett reduction.
     * @param s the scalar s, with pre-computed values for barrett reduction.
     * @return r * s mod modulus with the result with pre-computed values for barrett reduction.
     */
    public MultiplyUintModOperand mulRootScalar(MultiplyUintModOperand r, MultiplyUintModOperand s) {
        MultiplyUintModOperand result = new MultiplyUintModOperand();
        result.set(UintArithmeticSmallMod.multiplyUintMod(r.operand, s, modulus), modulus);
        return result;
    }

    /**
     * Ensures that a is in [0, 2 * modulus - 1). This is only valid when a is in [0, 2 * (2 * modulus - 1)).
     *
     * @param a a.
     * @return a is in [0, 2 * modulus - 1).
     */
    public long guard(long a) {
        return a >= twoTimesModulus ? a - twoTimesModulus : a;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }
}
