package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;

import java.util.Arrays;

/**
 * Uint arithmetic under a small modulus. A small modulus means that the modulus is at most 61-bit value.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/uintarithsmallmod.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/5
 */
public class UintArithmeticSmallMod {
    /**
     * private constructor.
     */
    private UintArithmeticSmallMod() {
        // empty
    }

    /**
     * Computes Σ_i (a_i * b_i) mod modulus, i ∈ [0, count).
     *
     * @param operand1 operand1 with uint64 value array.
     * @param operand2 operand2 with uint64 value array.
     * @param count    number of uint64 values in operand1 and operand2.
     * @param modulus  modulus.
     * @return Σ_i (a_i * b_i) mod modulus, i ∈ [0, count).
     */
    public static long dotProductMod(long[] operand1, long[] operand2, int count, Modulus modulus) {
        assert count >= 0;
        long[] accumulator = new long[2];
        switch (count) {
            case 0:
                return 0;
            case 1:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 1);
                break;
            case 2:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 2);
                break;
            case 3:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 3);
                break;
            case 4:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 4);
                break;
            case 5:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 5);
                break;
            case 6:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 6);
                break;
            case 7:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 7);
                break;
            case 8:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 8);
                break;
            case 9:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 9);
                break;
            case 10:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 10);
                break;
            case 11:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 11);
                break;
            case 12:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 12);
                break;
            case 13:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 13);
                break;
            case 14:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 14);
                break;
            case 15:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 15);
                break;
            case 16:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 16);
                break;
            default:
                long[] c1 = Arrays.copyOfRange(operand1, 16, count);
                long[] c2 = Arrays.copyOfRange(operand2, 16, count);
                accumulator[0] = dotProductMod(c1, c2, count - 16, modulus);
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 16);
                break;
        }
        return barrettReduce128(accumulator, modulus);
    }

    /**
     * Computes a^e mod modulus.
     *
     * @param operand  an uint64 operand representing a.
     * @param exponent an uint64 exponent representing e.
     * @param modulus  modulus.
     * @return a^e mod modulus.
     */
    public static long exponentUintMod(long operand, long exponent, Modulus modulus) {
        assert !modulus.isZero();
        assert operand < modulus.value();
        if (exponent == 0) {
            return 1;
        }
        if (exponent == 1) {
            return operand;
        }
        // Perform binary exponentiation.
        long power = operand;
        long product;
        long intermediate = 1;
        // Initially: power = operand and intermediate = 1, product is irrelevant.
        while (true) {
            if ((exponent & 1) > 0) {
                product = multiplyUintMod(power, intermediate, modulus);
                // update intermediate
                intermediate = product;
            }
            exponent >>>= 1;
            if (exponent == 0) {
                break;
            }
            product = multiplyUintMod(power, power, modulus);
            // update power
            power = product;
        }
        return intermediate;
    }

    /**
     * Computes a^{-1} mod modulus and store it in result[0].
     *
     * @param operand an uint64 operand representing a.
     * @param modulus modulus.
     * @param result  result[0] stores a^{-1} mod modulus.
     * @return true if a^{-1} exists; otherwise, return false.
     */
    public static boolean tryInvertUintMod(long operand, Modulus modulus, long[] result) {
        return Numth.tryInvertUintMod(operand, modulus.value(), result);
    }

    /**
     * Computes a^{-1} mod modulus and store it in result[0].
     *
     * @param operand an uint64 operand representing a.
     * @param modulus modulus.
     * @param result  result[0] stores a^{-1} mod modulus.
     * @return true if a^{-1} exists; otherwise, return false.
     */
    public static boolean tryInvertUintMod(long operand, long modulus, long[] result) {
        return Numth.tryInvertUintMod(operand, modulus, result);
    }


    /**
     * Computes a mod modulus. Note that the result is stored in values[0], and values[1 .. uint64) is reduced to 0.
     *
     * @param operand     operand representing a.
     * @param uint64Count number of uint64 used in operand.
     * @param modulus     modulus.
     */
    public static void moduloUintInplace(long[] operand, int uint64Count, Modulus modulus) {
        assert operand != null;
        assert uint64Count > 0;

        if (uint64Count == 1) {
            if (operand[0] >= modulus.value()) {
                operand[0] = barrettReduce64(operand[0], modulus);
            }
            return;
        }
        int i = uint64Count - 1;
        long[] tmp = new long[2];
        // do modulo reduction from right to left
        while (i-- > 0) {
            System.arraycopy(operand, i, tmp, 0, 2);
            operand[i] = barrettReduce128(tmp, modulus);
            operand[i + 1] = 0;
        }
    }

    /**
     * Computes a mod modulus.
     *
     * @param operand     operand representing a.
     * @param uint64Count number of uint64 used in operand.
     * @param modulus     modulus.
     * @return a mod modulus.
     */
    public static long moduloUint(long[] operand, int uint64Count, Modulus modulus) {
        return moduloUint(operand, 0, uint64Count, modulus);
    }

    /**
     * Computes a mod modulus.
     *
     * @param operand     operand representing a.
     * @param startIndex  the start index in operand.
     * @param uint64Count number of uint64 used in operand.
     * @param modulus     modulus.
     * @return a mod modulus.
     */
    public static long moduloUint(long[] operand, int startIndex, int uint64Count, Modulus modulus) {
        assert uint64Count > 0;

        if (uint64Count == 1) {
            if (operand[startIndex] < modulus.value()) {
                return operand[startIndex];
            } else {
                return barrettReduce64(operand[startIndex], modulus);
            }
        }

        long[] tmp = new long[]{0, operand[startIndex + uint64Count - 1]};
        // do modulo reduction from right to left
        for (int i = startIndex + uint64Count - 1; i-- > startIndex; ) {
            tmp[0] = operand[i];
            tmp[1] = barrettReduce128(tmp, modulus);
        }
        return tmp[1];
    }


    /**
     * Computes (a * b) + c mod modulus.
     *
     * @param operand1 an uint64 operand representing a.
     * @param operand2 an uint64 operand representing b.
     * @param operand3 an uint64 operand representing c.
     * @param modulus  modulus.
     * @return (a * b) + c mod modulus.
     */
    public static long multiplyAddUintMod(long operand1, long operand2, long operand3, Modulus modulus) {
        long[] tmp = new long[2];
        UintArithmetic.multiplyUint64(operand1, operand2, tmp);
        long[] addTmp = new long[1];
        long carry = UintArithmetic.addUint64(tmp[0], operand3, addTmp);
        // update low 64 bits
        tmp[0] = addTmp[0];
        // add carry
        tmp[1] += carry;
        // mod reduce
        return barrettReduce128(tmp, modulus);
    }

    /**
     * Compute (a * b) + c mod modulus with (a * b) a highly-optimized variant of Barrett reduction, or called shoup-mul.
     *
     * @param operand1 an uint64 operand representing a.
     * @param operand2 an uint64 operand representing b, with pre-computed values for barrett reduction
     * @param operand3 an uint64 operand representing c.
     * @param modulus  modulus.
     * @return (operand1 * operand2) + operand3 mod modulus.
     */
    public static long multiplyAddUintMod(long operand1, MultiplyUintModOperand operand2, long operand3, Modulus modulus) {
        return addUintMod(multiplyUintMod(operand1, operand2, modulus), barrettReduce64(operand3, modulus), modulus);
    }


    /**
     * Computes (a * b) mod modulus.
     *
     * @param operand1 an uint64 operand representing a.
     * @param operand2 an uint64 operand representing b.
     * @param modulus  modulus.
     * @return (a * b) mod modulus.
     */
    public static long multiplyUintMod(long operand1, long operand2, Modulus modulus) {
        long[] z = new long[2];
        UintArithmetic.multiplyUint64(operand1, operand2, z);
        return barrettReduce128(z, modulus);
    }

    /**
     * Computes (a * b) mod modulus. This is a highly-optimized variant of Barrett reduction, or called shoup-mul.
     *
     * @param operand1 an uint64 operand representing a.
     * @param operand2 an uint64 operand representing b, with pre-computed values for barrett reduction.
     * @param modulus  modulus.
     * @return a * b mod modulus.
     */
    public static long multiplyUintMod(long operand1, MultiplyUintModOperand operand2, Modulus modulus) {
        assert operand2.operand < modulus.value() : "y: " + operand2.operand + ", modulus: " + modulus.value();

        long tmp1, tmp2;
        long p = modulus.value();
        tmp1 = UintArithmetic.multiplyUint64Hw64(operand1, operand2.quotient);
        tmp2 = operand2.operand * operand1 - tmp1 * p;
        return tmp2 >= p ? tmp2 - p : tmp2;
    }

    /**
     * Computes (a * b mod modulus) or (a * b mod modulus + modulus). This is a highly-optimized variant of Barrett
     * reduction and reduce to [0, 2 * modulus - 1].
     *
     * @param operand1 an uint64 operand representing a.
     * @param operand2 an uint64 operand representing b, with pre-computed values for barrett reduction.
     * @param modulus  modulus.
     * @return (a * b mod modulus) or (a * b mod modulus + modulus).
     */
    public static long multiplyUintModLazy(long operand1, MultiplyUintModOperand operand2, Modulus modulus) {
        assert operand2.operand < modulus.value();

        long tmp1;
        long p = modulus.value();
        tmp1 = UintArithmetic.multiplyUint64Hw64(operand1, operand2.quotient);
        // res \in [0, 2p)
        return operand2.operand * operand1 - tmp1 * p;
    }

    /**
     * Computes operand mod modulus.
     *
     * @param operand an uint64 operand.
     * @param modulus modulus.
     * @return operand mod modulus.
     */
    public static long barrettReduce64(long operand, Modulus modulus) {
        // Reduces operand using base 2^64 Barrett reduction
        // floor(2^64 / mod) == floor( floor(2^128 / mod) )
        long q = UintArithmetic.multiplyUint64Hw64(operand, modulus.constRatio()[1]);
        long res = operand - q * modulus.value();
        return res >= modulus.value() ? res - modulus.value() : res;
    }

    /**
     * Computes operand mod modulus.
     *
     * @param operand an uint128 operand.
     * @param modulus modulus.
     * @return operand mod modulus.
     */
    public static long barrettReduce128(long[] operand, Modulus modulus) {
        assert operand.length == 2;

        long tmp1, tmp3, carry;
        long[] tmp2 = new long[2];
        // (x0 * m0)_1 is the higher 64-bit of x0 * m0, (x0 * m0)_0 is the lower 64-bit of x0 * m0
        carry = UintArithmetic.multiplyUint64Hw64(operand[0], modulus.constRatio()[0]);
        // tmp2 = [(x0 * m1)_0, (x0 * m1)_1]
        UintArithmetic.multiplyUint64(operand[0], modulus.constRatio()[1], tmp2);
        // (x0 * m0)_1 + (x0 * m1)_0} >> 64
        long[] addTmp = new long[1];
        long carryTmp = UintArithmetic.addUint64(tmp2[0], carry, addTmp);
        tmp1 = addTmp[0];
        // tmp3 = (x0 * m1)_1 + carry
        tmp3 = tmp2[1] + carryTmp;

        // tmp2 = [(x1 * m0)_0, (x1 * m0)_1]
        UintArithmetic.multiplyUint64(operand[1], modulus.constRatio()[0], tmp2);
        carryTmp = UintArithmetic.addUint64(tmp1, tmp2[0], addTmp);
        // (x1 * m0)_1 + [(x0 * m1)_0 + (x0 * m0)_1] + carry for (x1 * m0)_0, which is (x1 * m0)_0 / 2^64 + x1 * m0
        carry = tmp2[1] + carryTmp;
        // x1 * m1 + (x0 * m1)_1 + (x1 * m0)_1 + carry
        tmp1 = operand[1] * modulus.constRatio()[1] + tmp3 + carry;
        // reduction
        tmp3 = operand[0] - tmp1 * modulus.value();

        return tmp3 >= modulus.value() ? tmp3 - modulus.value() : tmp3;
    }


    /**
     * Computes (operand + 1) mod modulus.
     *
     * @param operand an uint64 operand, at most (2 * modulus - 2).
     * @param modulus modulus.
     * @return (operand + 1) mod modulus.
     */
    public static long incrementUintMod(long operand, Modulus modulus) {
        assert Long.compareUnsigned(operand, (modulus.value() - 1) << 1) <= 0;

        operand++;
        return operand - (modulus.value() & ((operand >= modulus.value() ? -1 : 0)));
    }

    /**
     * Computes (operand - 1) mod modulus.
     *
     * @param operand an uint64 operand, at most (modulus - 1).
     * @param modulus modulus.
     * @return (operand - 1) mod modulus.
     */
    public static long decrementUintMod(long operand, Modulus modulus) {
        assert !modulus.isZero();
        assert Long.compareUnsigned(operand, modulus.value()) < 0;

        long carry = operand == 0 ? 1 : 0;
        return operand - 1 + (modulus.value() & -carry);
    }

    /**
     * Computes (-operand) mod modulus.
     *
     * @param operand an uint64 operand, at most modulus - 1.
     * @param modulus modulus.
     * @return (- operand) mod modulus.
     */
    public static long negateUintMod(long operand, Modulus modulus) {
        assert !modulus.isZero();
        assert Long.compareUnsigned(operand, modulus.value()) < 0;

        long nonZero = operand != 0 ? 1 : 0;
        return (modulus.value() - operand) & (-nonZero);
    }

    /**
     * Computes (operand / 2) mod modulus.
     *
     * @param operand an uint64 operand, at most modulus - 1.
     * @param modulus modulus.
     * @return (operand / 2) mod modulus.
     */
    public static long div2UintMod(long operand, Modulus modulus) {
        assert !modulus.isZero();
        assert Long.compareUnsigned(operand, modulus.value()) < 0;

        // odd value, a / 2 = a * inv(2) mod p = (a + p) >>> 1
        if ((operand & 1) > 0) {
            long[] tmp = new long[1];
            long carry = UintArithmetic.addUint64(operand, modulus.value(), 0, tmp);
            operand = tmp[0] >>> 1;
            // if we meet overflow, set the highest bit of operand to 1
            if (carry > 0) {
                return operand | (1L << (UintArithmetic.UINT64_BITS - 1));
            }
            return operand;
        }
        // even value
        return operand >>> 1;
    }

    /**
     * Computes (a + b) mod modulus.
     *
     * @param operand1 an uint64 operand representing a.
     * @param operand2 an uint64 operand representing b.
     * @param modulus  modulus.
     * @return (a + b) mod modulus.
     */
    public static long addUintMod(long operand1, long operand2, Modulus modulus) {
        assert !modulus.isZero();
        assert Long.compareUnsigned(operand1 + operand2, modulus.value() << 1) < 0;
        // Sum of a + b modulo Modulus can never wrap around 2^64
        operand1 += operand2;
        return operand1 >= modulus.value() ? operand1 - modulus.value() : operand1;
    }

    /**
     * Computes (a - b) mod modulus.
     *
     * @param operand1 an uint64 operand representing a.
     * @param operand2 an uint64 operand representing b.
     * @param modulus  modulus.
     * @return (a - b) mod modulus.
     */
    public static long subUintMod(long operand1, long operand2, Modulus modulus) {
        assert !modulus.isZero();
        assert Long.compareUnsigned(operand1, modulus.value()) < 0;
        assert Long.compareUnsigned(operand2, modulus.value()) < 0;

        // tmp[0] is sub result
        long[] tmp = new long[1];
        long borrow = UintArithmetic.subUint64(operand1, operand2, 0, tmp);
        // if borrow = 1, return tmp[0] + modulus
        // if borrow = 0, return tmp[0]
        return tmp[0] + (modulus.value() & (-borrow));
    }
}
