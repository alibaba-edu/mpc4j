package edu.alibaba.mpc4j.crypto.fhe.zq;

import java.util.Arrays;

/**
 * Unsigned int arithmetic, or base-2^64 arithmetic.
 * <p>Modification here is to use long in Java equivalent to uint64_t in C++ as the most basic data type.</p>
 * <p></p>
 * The implementation is from: https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/uintarithmod.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/5
 */
public class UintArithmetic {
    /**
     * private constructor.
     */
    private UintArithmetic() {
        // empty
    }

    /**
     * bit-count of an uint64 value or a long value
     */
    public static final int UINT64_BITS = 64;

    /**
     * Computes (operand1 and operand2) and stores it in result[0, uint64Count).
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, uint64Count) stores the result.
     */
    public static void andUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        assert uint64Count > 0;
        while (--uint64Count >= 0) {
            result[uint64Count] = operand1[uint64Count] & operand2[uint64Count];
        }
    }

    /**
     * Computes (operand1 or operand2) and stores it in result[0, uint64Count).
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, uint64Count) stores the result.
     */
    public static void orUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        assert uint64Count > 0;
        while (--uint64Count >= 0) {
            result[uint64Count] = operand1[uint64Count] | operand2[uint64Count];
        }
    }

    /**
     * Computes (operand1 xor operand2) and stores it in result[0, uint64Count).
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, uint64Count) stores the result.
     */
    public static void xorUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        assert uint64Count > 0;
        while (--uint64Count >= 0) {
            result[uint64Count] = operand1[uint64Count] ^ operand2[uint64Count];
        }
    }

    /**
     * Computes (~operand) and stores it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores the result.
     */
    public static void notUint(long[] operand, int uint64Count, long[] result) {
        assert uint64Count > 0;
        while (--uint64Count >= 0) {
            result[uint64Count] = ~operand[uint64Count];
        }
    }

    /**
     * Computes ⌈(operand + 1) / 2⌉ and store it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores the result.
     */
    public static void halfRoundUpUint(long[] operand, int uint64Count, long[] result) {
        if (uint64Count == 0) {
            return;
        }
        // Set result to (operand + 1) / 2. To prevent overflowing operand, right shift
        // and then increment result if low-bit of operand was set.
        long lowBitSet = operand[0] & 1;
        // note that we use >>> instead of >>, Because we treat long here as an unsigned int64,
        // we need to use the corresponding logical right shift to ignore the sign bit.
        for (int i = 0; i < uint64Count - 1; i++) {
            result[i] = (operand[i] >>> 1) | (operand[i + 1] << (UINT64_BITS - 1));
        }
        result[uint64Count - 1] = operand[uint64Count - 1] >>> 1;
        // we expect the result is (operand / 2) + 0.5.
        // if lowBitSet = 0, then 0/2 + 0.5 ---> 0; if lowBitSet = 1, then 1/2 + 0.5 ---> 1.
        if (lowBitSet > 0) {
            incrementUint(result, uint64Count, result);
        }
    }

    /**
     * Computes (operand >> shiftAmount) and store it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param shiftAmount bit-count of right shift.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores the result.
     */
    public static void rightShiftUint(long[] operand, int shiftAmount, int uint64Count, long[] result) {
        assert uint64Count > 0;
        assert shiftAmount >= 0 && shiftAmount <= uint64Count * UINT64_BITS;
        // How many words to shift, one words is 64 bits
        int uint64ShiftAmount = shiftAmount / UINT64_BITS;
        // shift words
        System.arraycopy(operand, uint64ShiftAmount, result, 0, uint64Count - uint64ShiftAmount);
        Arrays.fill(result, uint64Count - uint64ShiftAmount, uint64Count, 0L);
        // shift bits
        int bitShiftAmount = shiftAmount - (uint64ShiftAmount * UINT64_BITS);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            for (int i = 0; i < uint64Count - 1; i++) {
                result[i] = (result[i] >>> bitShiftAmount) | (result[i + 1] << negBitShiftAmount);
            }
            result[uint64Count - 1] = result[uint64Count - 1] >>> bitShiftAmount;
        }
    }

    /**
     * Computes (operand << shiftAmount) and store it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param shiftAmount bit-count of left shift.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores the result.
     */
    public static void leftShiftUint(long[] operand, int shiftAmount, int uint64Count, long[] result) {
        assert uint64Count > 0;
        assert shiftAmount >= 0 && shiftAmount <= uint64Count * UINT64_BITS;
        // How many words to shift, one words is 64 bits
        int uint64ShiftAmount = shiftAmount / UINT64_BITS;
        // shift words
        for (int i = 0; i < uint64Count - uint64ShiftAmount; i++) {
            result[uint64Count - i - 1] = operand[uint64Count - i - 1 - uint64ShiftAmount];
        }
        for (int i = uint64Count - uint64ShiftAmount; i < uint64Count; i++) {
            result[uint64Count - i - 1] = 0;
        }
        // shift bits
        int bitShiftAmount = shiftAmount - (uint64ShiftAmount * UINT64_BITS);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            for (int i = uint64Count - 1; i > 0; i--) {
                result[i] = (result[i] << bitShiftAmount) | (result[i - 1] >>> negBitShiftAmount);
            }
            result[0] = result[0] << bitShiftAmount;
        }
    }

    /**
     * Computes -operand and store it in result[0, uint64Count), where negation is inverting bits and adding 1.
     *
     * @param operand     operand.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores the result.
     */
    public static void negateUint(long[] operand, int uint64Count, long[] result) {
        assert uint64Count > 0;
        long[] tmp = new long[1];
        long carry;
        // Negation is equivalent to inverting bits and adding 1.
        carry = addUint64(~operand[0], 1, tmp);
        result[0] = tmp[0];
        int i = 1;
        while (--uint64Count > 0) {
            carry = addUint64(~operand[i], 0, carry, tmp);
            result[i] = tmp[0];
            i++;
        }
    }

    /**
     * Computes (operand - 1) -> (diff, borrow), diff is stored in result[0, uint64Count) and borrow is returned.
     *
     * @param operand     operand.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores the result.
     * @return the borrow (0 or 1).
     */
    public static long decrementUint(long[] operand, int uint64Count, long[] result) {
        assert uint64Count > 0;
        return subUint(operand, uint64Count, 1, result);
    }

    /**
     * Computes (operand + 1) -> (sum, carry), sum is stored in result[0, uint64Count) and carry is returned.
     *
     * @param operand     operand.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores the result.
     * @return the carry (0 or 1).
     */
    public static long incrementUint(long[] operand, int uint64Count, long[] result) {
        assert uint64Count > 0;
        return addUint(operand, uint64Count, 1, result);
    }


    /**
     * Computes (operand1 + operand2) -> (sum, carry), sum is stored in result[0, 2), and carry is returned.
     *
     * @param operand1 an unsigned 128-bit operand1.
     * @param operand2 an unsigned 128-bit operand2.
     * @param result   result[0, 1] stores (operand1 + operand2)'s low 128-bit value.
     * @return the carry (0 or 1).
     */
    public static long addUint128(long[] operand1, long[] operand2, long[] result) {
        long[] tmp = new long[1];
        long carry = addUint64(operand1[0], operand2[0], tmp);
        result[0] = tmp[0];
        carry = addUint64(operand1[1], operand2[1], carry, tmp);
        result[1] = tmp[0];

        return carry;
    }

    /**
     * Computes (operand1 * operand2) and store it in result[0, uint64Count), high bits is truncated.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, uint64Count) stores the result.
     */
    public static void multiplyTruncateUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        multiplyUint(operand1, uint64Count, operand2, uint64Count, uint64Count, result);
    }

    /**
     * Computes (operand1 * operand2) and store it in result[0, 2 * uint64Count).
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, 2 * uint64Count) stores the result.
     */
    public static void multiplyUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        multiplyUint(operand1, uint64Count, operand2, uint64Count, uint64Count * 2, result);
    }

    /**
     * Computes (operand1 * operand2) and store it in result[0, resultUint64Count).
     *
     * @param operand1            operand1.
     * @param operand1Uint64Count number of uint64 in operand1.
     * @param operand2            operand2.
     * @param operand2Uint64Count number of uint64 in operand2.
     * @param resultUint64Count   number of uint64 in result.
     * @param result              result[0, resultUint64Count) stores the result.
     */
    public static void multiplyUint(long[] operand1, int operand1Uint64Count,
                                    long[] operand2, int operand2Uint64Count,
                                    int resultUint64Count, long[] result) {
        assert operand1Uint64Count >= 0;
        assert operand2Uint64Count >= 0;
        assert resultUint64Count > 0;
        assert result != operand1 && result != operand2;

        if (operand1Uint64Count == 0 || operand2Uint64Count == 0) {
            UintCore.setZeroUint(resultUint64Count, result);
            return;
        }
        if (resultUint64Count == 1) {
            result[0] = operand1[0] * operand2[0];
            return;
        }
        // only handle necessary bits. For example, [1, 0, 0] * [1, 0] with uint64Count = 1, we only handle 1 * 1 = 1.
        operand1Uint64Count = UintCore.getSignificantUint64CountUint(operand1, operand1Uint64Count);
        operand2Uint64Count = UintCore.getSignificantUint64CountUint(operand2, operand2Uint64Count);
        // more fast
        if (operand1Uint64Count == 1) {
            multiplyUint(operand2, operand2Uint64Count, operand1[0], resultUint64Count, result);
            return;
        }
        if (operand2Uint64Count == 1) {
            multiplyUint(operand1, operand1Uint64Count, operand2[0], resultUint64Count, result);
            return;
        }
        // clear result
        UintCore.setZeroUint(resultUint64Count, result);
        // long[] * long[]
        int asIndexMax = Math.min(operand1Uint64Count, resultUint64Count);
        for (int asIndex = 0; asIndex < asIndexMax; asIndex++) {
            long[] innerBs = new long[operand2.length];
            System.arraycopy(operand2, 0, innerBs, 0, operand2.length);
            long[] innerResult = new long[resultUint64Count - asIndex];
            // create new innerResult from result, note that result's start index is asIndex
            System.arraycopy(result, asIndex, innerResult, 0, innerResult.length);
            long carry = 0;
            int bsIndex = 0;
            int bsIndexMax = Math.min(operand2Uint64Count, resultUint64Count - asIndex);
            for (; bsIndex < bsIndexMax; bsIndex++) {
                long[] tempResult = new long[2];
                multiplyUint64(operand1[asIndex], innerBs[bsIndex], tempResult);
                long[] addTemp = new long[1];
                long tmpCarry = addUint64(tempResult[0], carry, 0, addTemp);
                carry = tempResult[1] + tmpCarry;
                long[] addTemp2 = new long[1];
                long tmpCarry2 = addUint64(innerResult[bsIndex], addTemp[0], 0, addTemp2);
                carry += tmpCarry2;
                innerResult[bsIndex] = addTemp2[0];
            }
            // Write carry if there is room in result
            if (asIndex + bsIndexMax < resultUint64Count) {
                innerResult[bsIndex] = carry;
            }
            // overwrite result, note that result's start index is asIndex
            System.arraycopy(innerResult, 0, result, asIndex, innerResult.length);
        }
    }

    /**
     * Computes (operand * scalar) and store it in result[0, resultUint64Count).
     *
     * @param operand            operand.
     * @param operandUint64Count number of uint64 in operand.
     * @param b                  unsigned scalar.
     * @param resultUint64Count  number of uint64 in result.
     * @param result             result[0, resultUint64Count) stores the result.
     */
    public static void multiplyUint(long[] operand, int operandUint64Count, long b,
                                    int resultUint64Count, long[] result) {
        assert operandUint64Count >= 0;
        assert resultUint64Count > 0;
        assert result != operand;
        if (operandUint64Count == 0 || b == 0) {
            Arrays.fill(result, 0, resultUint64Count, 0);
            return;
        }
        if (resultUint64Count == 1) {
            // since resultUint64Count is 1, we can directly ignore overflow.
            result[0] = operand[0] * b;
            return;
        }
        // clear out result
        UintCore.setZeroUint(resultUint64Count, result);
        // Multiply
        long carry = 0;
        int asIndexMax = Math.min(operandUint64Count, resultUint64Count);

        int asIndex = 0;
        for (; asIndex < asIndexMax; asIndex++) {
            // index = 0 is the low-64bit, index = 1 is the high 64 bits
            long[] mulTemp = new long[2];
            multiplyUint64(operand[asIndex], b, mulTemp);
            // addTemp[0] is the add result, the low 64-bit, carryTemp store the carry
            long[] addTemp = new long[1];
            // add two carries
            long carryTemp = addUint64(mulTemp[0], carry, 0, addTemp);
            carry = mulTemp[1] + carryTemp;
            result[asIndex] = addTemp[0];
        }
        if (asIndexMax < resultUint64Count) {
            result[asIndex] = carry;
        }
    }

    /**
     * Computes Π_{i = 0}^{count - 1} operands[i] and store it in result[0, count).
     *
     * @param operands an array, each value is an uint64 value.
     * @param count    number of multiplied elements.
     * @param result   result[0, count) stores Π_{i = 0}^{count - 1} operands[i].
     */
    public static void multiplyManyUint64(long[] operands, int count, long[] result) {
        assert operands != result;
        if (count == 0) {
            return;
        }
        result[0] = operands[0];
        long[] tempMpi = new long[count];
        for (int i = 1; i < count; i++) {
            multiplyUint(result, i, operands[i], i + 1, tempMpi);
            UintCore.setUint(tempMpi, i + 1, result);
        }
    }

    /**
     * Computes Π_{i = 0}^{count - 1, i != except} operands[i] and store it in result[0, count).
     *
     * @param operands an array, each value is an uint64 value.
     * @param count    number of multiplied elements.
     * @param except   operands[except] will not be multiplied.
     * @param result   result[0, count) stores Π_{i = 0}^{count - 1, i != except} operands[i].
     */
    public static void multiplyManyUint64Except(long[] operands, int count, int except, long[] result) {
        assert operands != result;
        assert count >= 1;
        assert except >= 0 && except < count;

        // empty product, res = 1, when count == 1, valid except must be 0 since 0 <= except < count
        if (count == 1) {
            result[0] = 1;
            return;
        }
        // set result is operand[0] unless except = 0
        result[0] = except == 0 ? 1 : operands[0];
        long[] tempMpi = new long[count];
        for (int i = 1; i < count; i++) {
            if (i != except) {
                multiplyUint(result, i, operands[i], i + 1, tempMpi);
                UintCore.setUint(tempMpi, i + 1, result);
            }
        }
    }

    /**
     * Computes a * b = r = r0 + r1 * 2^64, and store it in result[0, 2). The basic idea is:
     * a = (2^32 * a1 + a0), b = (2^32 * b1 + b0); then a * b = (2^32 * a1 + a0) * (2^32 * b1 + b0).
     *
     * @param a         a.
     * @param b         b.
     * @param result128 result[0, 2) store a * b.
     */
    public static void multiplyUint64(long a, long b, long[] result128) {
        multiplyUint64Generic(a, b, result128);
    }

    /**
     * Computes a * b = r = r0 + r1 * 2^64, and store it in result[0, 2). The basic idea is:
     * a = (2^32 * a1 + a0), b = (2^32 * b1 + b0); then a * b = (2^32 * a1 + a0) * (2^32 * b1 + b0).
     *
     * @param a         a.
     * @param b         b.
     * @param result128 result[0, 2) store a * b.
     */
    public static void multiplyUint64Generic(long a, long b, long[] result128) {
        long aRight = a & 0x00000000FFFFFFFFL;
        long bRight = b & 0x00000000FFFFFFFFL;
        a >>>= 32;
        b >>>= 32;

        long middle1 = a * bRight;
        long middle, carry;
        long[] tmp = new long[1];
        carry = addUint64(middle1, b * aRight, tmp);
        middle = tmp[0];

        long left = a * b + (carry << 32);
        long right = aRight * bRight;
        long tmpSum = (right >>> 32) + (middle & 0x00000000FFFFFFFFL);

        result128[1] = left + (middle >>> 32) + (tmpSum >>> 32);
        result128[0] = (tmpSum << 32) | (right & 0x00000000FFFFFFFFL);
    }

    /**
     * Computes and returns (a * b)'s high 64-bit result.
     *
     * @param a a.
     * @param b b.
     * @return (a * b)'s high 64-bit result.
     */
    public static long multiplyUint64Hw64(long a, long b) {
        return multiplyUint64Hw64Generic(a, b);
    }

    /**
     * Computes and returns (a * b)'s high 64-bit result.
     *
     * @param a a.
     * @param b b.
     * @return (a * b)'s high 64-bit result.
     */
    public static long multiplyUint64Hw64Generic(long a, long b) {
        long result;
        long aRight = a & 0x00000000FFFFFFFFL;
        long bRight = b & 0x00000000FFFFFFFFL;
        a >>>= 32;
        b >>>= 32;

        long middle1 = a * bRight;
        long middle, carry;
        long[] tmp = new long[1];
        carry = addUint64(middle1, b * aRight, tmp);
        middle = tmp[0];
        long left = a * b + (carry << 32);
        long right = aRight * bRight;
        long tmpSum = (right >>> 32) + (middle & 0x00000000FFFFFFFFL);

        result = left + (middle >>> 32) + (tmpSum >>> 32);
        return result;
    }

    /**
     * Computes Σ_{i = 0}^{count - 1} (operand1[i] * operand2[i]) and store it in accumulator[0, count).
     *
     * @param operand1    an array, each value is an unsigned value.
     * @param startIndex1 start index of operand1.
     * @param operand2    an array, each value is an unsigned value.
     * @param startIndex2 start index of operand2.
     * @param accumulator store Σ_{i = 0}^{count - 1} (operand1[i] * operand2[i]).
     * @param count       number of elements in operand1 and operand2 that need to participate in the multiply.
     */
    public static void multiplyAccumulateUint64(long[] operand1, int startIndex1,
                                                long[] operand2, int startIndex2,
                                                long[] accumulator, int count) {
        if (count == 0) {
            return;
        }
        long[] qWord = new long[2];
        multiplyUint64(operand1[startIndex1], operand2[startIndex2], qWord);
        // using startIndex to avoid array copy
        multiplyAccumulateUint64(operand1, startIndex1 + 1, operand2, startIndex2 + 1, accumulator, count - 1);
        addUint128(qWord, accumulator, accumulator);
    }

    /**
     * Computes a / b = q + r, where a is numerator, b is denominator, q is stored in quotient[0, uint64Count),
     * and r is stored in num remainder[0, uint64Count).
     *
     * @param numerator   numerator.
     * @param denominator denominator.
     * @param uint64Count number of uint64 in numerator and denominator.
     * @param quotient    quotient[0, uint64Count) stores (a / b)'s quotient.
     * @param remainder   remainder[0, uint64Count) stores (a / )'s remainder.
     */
    public static void divideUint(long[] numerator, long[] denominator, int uint64Count, long[] quotient, long[] remainder) {
        UintCore.setUint(numerator, uint64Count, remainder);
        divideUintInplace(remainder, denominator, uint64Count, quotient);
    }

    /**
     * Computes a / b = q + r, where a is numerator, b is denominator, q is stored in quotient[0, uint64Count),
     * and r is stored in numerator[0, uint64Count).
     *
     * @param numerator   numerator, where r will be stored in numerator[0, uint64Count).
     * @param denominator denominator.
     * @param uint64Count number of uint64 in numerator and denominator.
     * @param quotient    quotient[0, uint64Count) stores (a / b)'s quotient.
     */
    public static void divideUintInplace(long[] numerator, long[] denominator, int uint64Count, long[] quotient) {
        assert uint64Count >= 0;
        assert quotient != numerator && quotient != denominator;

        if (uint64Count == 0) {
            return;
        }
        UintCore.setZeroUint(uint64Count, quotient);

        // significant bits
        int numeratorBits = UintCore.getSignificantBitCountUint(numerator, uint64Count);
        int denominatorBits = UintCore.getSignificantBitCountUint(denominator, uint64Count);
        // If numerator has fewer bits than denominator, then done.
        if (numeratorBits < denominatorBits) {
            return;
        }
        // Only perform computation up to last non-zero uint64s.
        uint64Count = UintCore.divideRoundUp(numeratorBits, UINT64_BITS);
        // if there is only one 64 bits, then directly use long to compute
        if (uint64Count == 1) {
            quotient[0] = numerator[0] / denominator[0];
            numerator[0] -= quotient[0] * denominator[0];
            return;
        }

        long[] shiftedDenominator = new long[uint64Count];
        // difference is the updated numerator
        long[] difference = new long[uint64Count];
        int denominatorShift = numeratorBits - denominatorBits;
        leftShiftUint(denominator, denominatorShift, uint64Count, shiftedDenominator);
        denominatorBits += denominatorShift;

        int remainingShifts = denominatorShift;
        while (numeratorBits == denominatorBits) {
            long borrow = subUint(numerator, shiftedDenominator, uint64Count, difference);
            if (borrow > 0) {
                // numerator < shifted_denominator and MSBs are aligned,
                // so current quotient bit is zero and next one is definitely one.

                if (remainingShifts == 0) {
                    break;
                }
                // Effectively shift numerator left by 1 by instead adding
                // numerator to difference (to prevent overflow in numerator).
                addUint(difference, numerator, uint64Count, difference);
                // Adjust quotient and remaining shifts as a result of
                // shifting numerator.
                leftShiftUint(quotient, 1, uint64Count, quotient);
                remainingShifts--;
            }
            // Difference is the new numerator with denominator subtracted.

            // Update quotient to reflect subtraction.
            quotient[0] |= 1;
            // Determine amount to shift numerator to bring MSB in alignment with denominator.
            numeratorBits = UintCore.getSignificantBitCountUint(difference, uint64Count);
            int numeratorShift = denominatorBits - numeratorBits;
            if (numeratorShift > remainingShifts) {
                // Clip the maximum shift to determine only the integer
                // (as opposed to fractional) bits.
                numeratorShift = remainingShifts;
            }
            // Shift and update numerator.
            if (numeratorBits > 0) {
                leftShiftUint(difference, numeratorShift, uint64Count, numerator);
                numeratorBits += numeratorShift;
            } else {
                // if numeratorBits = 0, mean difference = 0, so numerator = 0
                UintCore.setZeroUint(uint64Count, numerator);
            }
            // Adjust quotient and remaining shifts as a result of shifting numerator.
            leftShiftUint(quotient, numeratorShift, uint64Count, quotient);
            remainingShifts -= numeratorShift;
        }
        // Correct numerator (which is also the remainder) for shifting of denominator, unless it is just zero.
        if (numeratorBits > 0) {
            rightShiftUint(numerator, denominatorShift, uint64Count, numerator);
        }
    }

    /**
     * Computes a / b = q + r, where a is an unsigned 128-bit numerator, b is an unsigned 64-bit denominator,
     * q is stored in quotient[0, 2) and r is stored in numerator[0].
     *
     * @param numerator   an unsigned 128-bit numerator, where r will be stored in numerator[0].
     * @param denominator an unsigned 64-bit denominator.
     * @param quotient    quotient[0, 2) stores the (a / b)'s quotient.
     */
    public static void divideUint128Uint64InplaceGeneric(long[] numerator, long denominator, long[] quotient) {
        assert numerator != null;
        assert denominator != 0;
        assert quotient != null;
        assert numerator != quotient;

        // expect 128 bits input
        int uint64Count = 2;
        quotient[0] = 0;
        quotient[1] = 0;
        int numeratorBits = UintCore.getSignificantBitCountUint(numerator, uint64Count);
        int denominatorBits = UintCore.getSignificantBitCount(denominator);
        if (numeratorBits < denominatorBits) {
            return;
        }
        uint64Count = UintCore.divideRoundUp(numeratorBits, UINT64_BITS);
        if (uint64Count == 1) {
            // q = a / b
            quotient[0] = numerator[0] / denominator;
            // r = a - q * b
            numerator[0] -= quotient[0] * denominator;
            return;
        }
        long[] shiftedDenominator = new long[uint64Count];
        shiftedDenominator[0] = denominator;
        long[] difference = new long[uint64Count];
        int denominatorShift = numeratorBits - denominatorBits;
        leftShiftUint128(shiftedDenominator, denominatorShift, shiftedDenominator);
        denominatorBits += denominatorShift;

        // Perform bit-wise division algorithm.
        int remainingShifts = denominatorShift;
        while (numeratorBits == denominatorBits) {
            // difference = numerator - shiftedDenominator
            long borrow = subUint(numerator, shiftedDenominator, uint64Count, difference);
            if (borrow > 0) {
                if (remainingShifts == 0) {
                    break;
                }
                // difference = difference + numerator
                addUint(difference, numerator, uint64Count, difference);

                quotient[1] = (quotient[1] << 1) | (quotient[0] >>> (UINT64_BITS - 1));
                quotient[0] <<= 1;
                remainingShifts--;
            }
            numeratorBits = UintCore.getSignificantBitCountUint(difference, uint64Count);
            int numeratorShift = Math.min(denominatorBits - numeratorBits, remainingShifts);
            numerator[0] = 0;
            numerator[1] = 0;

            if (numeratorBits > 0) {
                leftShiftUint128(difference, numeratorShift, numerator);
                numeratorBits += numeratorShift;
            }
            quotient[0] |= 1;

            leftShiftUint128(quotient, numeratorShift, quotient);
            remainingShifts -= numeratorShift;
        }
        if (numeratorBits > 0) {
            rightShiftUint128(numerator, denominatorShift, numerator);
        }
    }

    /**
     * Compute a / b = q + r, where a is an unsigned 128-bit numerator, b is an unsigned 64-bit denominator,
     * q is stored in quotient[0, 2), and r is stored in numerator[0].
     *
     * @param numerator   an unsigned 128-bit numerator, where r will be stored in numerator[0].
     * @param denominator an unsigned 64-bit denominator.
     * @param quotient    quotient[0, 2) stores the (a / b)'s quotient.
     */
    public static void divideUint128Inplace(long[] numerator, long denominator, long[] quotient) {
        divideUint128Uint64InplaceGeneric(numerator, denominator, quotient);
    }

    /**
     * Computes a / b = q + r, where a is an unsigned 192-bit numerator, b is an unsigned 64-bit denominator,
     * q is stored in quotient[0, 3), and r is stored in numerator[0].
     *
     * @param numerator   an unsigned 128-bit numerator, where r will be stored in numerator[0].
     * @param denominator an unsigned 64-bit denominator.
     * @param quotient    quotient[0, 3) stores the (a / b)'s quotient.
     */
    public static void divideUint192Inplace(long[] numerator, long denominator, long[] quotient) {
        assert numerator != null;
        assert denominator != 0;
        assert quotient != null;
        assert numerator != quotient;

        int uint64Count = 3;
        quotient[0] = 0;
        quotient[1] = 0;
        quotient[2] = 0;

        int numeratorBits = UintCore.getSignificantBitCountUint(numerator, uint64Count);
        int denominatorBits = UintCore.getSignificantBitCount(denominator);

        if (numeratorBits < denominatorBits) {
            return;
        }
        uint64Count = UintCore.divideRoundUp(numeratorBits, UINT64_BITS);
        if (uint64Count == 1) {
            quotient[0] = numerator[0] / denominator;
            numerator[0] -= quotient[0] * denominator;
            return;
        }

        long[] shiftedDenominator = new long[uint64Count];
        shiftedDenominator[0] = denominator;

        long[] difference = new long[uint64Count];
        int denominatorShift = numeratorBits - denominatorBits;

        leftShiftUint192(shiftedDenominator, denominatorShift, shiftedDenominator);

        denominatorBits += denominatorShift;

        int remainingShifts = denominatorShift;

        while (numeratorBits == denominatorBits) {
            long borrow = subUint(numerator, shiftedDenominator, uint64Count, difference);
            if (borrow > 0) {
                if (remainingShifts == 0) {
                    break;
                }
                addUint(difference, numerator, uint64Count, difference);
                // quotient << 1
                leftShiftUint192(quotient, 1, quotient);
                remainingShifts--;
            }
            quotient[0] |= 1;

            numeratorBits = UintCore.getSignificantBitCountUint(difference, uint64Count);
            int numeratorShift = Math.min(denominatorBits - numeratorBits, remainingShifts);

            if (numeratorBits > 0) {
                leftShiftUint192(difference, numeratorShift, numerator);
                numeratorBits += numeratorShift;
            } else {
                UintCore.setZeroUint(uint64Count, numerator);
            }

            leftShiftUint192(quotient, numeratorShift, quotient);
            remainingShifts -= numeratorShift;
        }

        if (numeratorBits > 0) {
            rightShiftUint192(numerator, denominatorShift, numerator);
        }
    }

    /**
     * Computes (operand >> shiftAmount) and store it in result[0, 2).
     *
     * @param operand     an unsigned 128-bit operand.
     * @param shiftAmount bit-count of right shift.
     * @param result      result[0, 2) stores the (operand >> shiftAmount).
     */
    public static void rightShiftUint128(long[] operand, int shiftAmount, long[] result) {
        assert operand.length == 2;
        assert shiftAmount >= 0 && shiftAmount <= 3 * UINT64_BITS;
        assert operand.length == result.length;

        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }

        // shiftAmount >= 64-bit, one word shift
        if ((shiftAmount & UINT64_BITS) > 0) {
            result[0] = operand[1];
            result[1] = 0;
        } else {
            // shiftAmount in [0，64) no word shift
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount & (UINT64_BITS - 1);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            result[0] = (result[0] >>> bitShiftAmount) | (result[1] << negBitShiftAmount);
            result[1] = result[1] >>> bitShiftAmount;
        }
    }

    /**
     * Computes (operand >> shiftAmount) and store it in result[0, 3).
     *
     * @param operand     an unsigned 192-bit operand.
     * @param shiftAmount bit-count of right shift.
     * @param result      result[0, 3) store the (operand >> shiftAmount).
     */
    public static void rightShiftUint192(long[] operand, int shiftAmount, long[] result) {
        assert operand.length == 3;
        assert shiftAmount >= 0 && shiftAmount <= 3 * UINT64_BITS;
        assert operand.length == result.length;

        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }

        if ((shiftAmount & UINT64_BITS << 1) > 0) {
            // shiftAmount >= 128
            result[0] = operand[2];
            result[1] = 0;
            result[2] = 0;
        } else if ((shiftAmount & UINT64_BITS) > 0) {
            // shiftAmount in [64, 128)
            result[0] = operand[1];
            result[1] = operand[2];
            result[2] = 0;
        } else {
            // shiftAmount in [0, 64)
            result[2] = operand[2];
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount & (UINT64_BITS - 1);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            result[0] = (result[0] >>> bitShiftAmount) | (result[1] << negBitShiftAmount);
            result[1] = (result[1] >>> bitShiftAmount) | (result[2] << negBitShiftAmount);
            result[2] = result[2] >>> bitShiftAmount;
        }
    }

    /**
     * Computes (operand << shiftAmount) and store it in result[0, 2).
     *
     * @param operand     an unsigned 128-bit operand.
     * @param shiftAmount the bit count of left shift.
     * @param result      result[0, 2) store the (operand << shiftAmount).
     */
    public static void leftShiftUint128(long[] operand, int shiftAmount, long[] result) {
        assert shiftAmount >= 0 && shiftAmount <= 2 * UINT64_BITS;

        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }
        if ((shiftAmount & UINT64_BITS) > 0) {
            // shiftAmount >= 64
            result[1] = operand[0];
            result[0] = 0;
        } else {
            // shiftAmount in [0, 64)
            result[1] = operand[1];
            result[0] = operand[0];
        }
        // compute shiftAmount % 64
        int bitShiftAmount = shiftAmount & (UINT64_BITS - 1);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            // high-bit shift left, and padding with n low-bit
            result[1] = (result[1] << bitShiftAmount) | (result[0] >>> negBitShiftAmount);
            // low-bit shift left
            result[0] = result[0] << bitShiftAmount;
        }
    }

    /**
     * Computes (operand << shiftAmount) and store it in result[0, 2).
     *
     * @param operand     an unsigned 192-bit operand.
     * @param shiftAmount the bit count of left shift.
     * @param result      result[0, 3) store the (operand << shiftAmount).
     */
    public static void leftShiftUint192(long[] operand, int shiftAmount, long[] result) {
        assert shiftAmount >= 0 && shiftAmount <= 3 * UINT64_BITS;
        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }
        if ((shiftAmount & (UINT64_BITS << 1)) > 0) {
            // shiftAmount >= 128
            result[2] = operand[0];
            result[1] = 0;
            result[0] = 0;
        } else if ((shiftAmount & UINT64_BITS) > 0) {
            // shiftAmount in [64, 128)
            result[2] = operand[1];
            result[1] = operand[0];
            result[0] = 0;
        } else {
            // shiftAmount in [0, 64)
            result[2] = operand[2];
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount & (UINT64_BITS - 1);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            // right shift must be use unsigned right shift
            result[2] = (result[2] << bitShiftAmount) | (result[1] >>> negBitShiftAmount);
            result[1] = (result[1] << bitShiftAmount) | (result[0] >>> negBitShiftAmount);
            result[0] = result[0] << bitShiftAmount;
        }
    }

    /**
     * Computes (operand1 - operand2) -> (diff, borrow), diff is stored in result[0], borrow is returned.
     *
     * @param operand1 an unsigned 64-bit operand1.
     * @param operand2 an unsigned 64-bit operand2.
     * @param result   result[0] stores the (operand1 - operand2).
     * @return (operand1 - operand2)'s borrow (0 or 1).
     */
    public static long subUint64(long operand1, long operand2, long[] result) {
        result[0] = operand1 - operand2;
        // b > a must produce borrow
        return Long.compareUnsigned(operand2, operand1) > 0 ? 1 : 0;
    }

    /**
     * Computes (operand1 - operand2 - borrow) -> (diff, borrow), diff is stored in result[0], borrow is returned.
     *
     * @param operand1 an unsigned 64-bit operand1.
     * @param operand2 an unsigned 64-bit operand2.
     * @param borrow   the given borrow (0 or 1).
     * @param result   result[0] store the (operand1 - operand2 - borrow).
     * @return (operand1 - operand2 - borrow)'s borrow (0 or 1).
     */
    public static long subUint64(long operand1, long operand2, long borrow, long[] result) {
        return subUint64Generic(operand1, operand2, borrow, result);
    }

    /**
     * Computes (operand1 - operand2 - borrow) -> (diff, borrow), diff is stored in result[0], borrow is returned.
     *
     * @param operand1 an unsigned 64-bit operand1.
     * @param operand2 an unsigned 64-bit operand2.
     * @param borrow   the given borrow (0 or 1).
     * @param result   result[0] store the (operand1 - operand2 - borrow).
     * @return (operand1 - operand2 - borrow)'s borrow (0 or 1).
     */
    public static long subUint64Generic(long operand1, long operand2, long borrow, long[] result) {
        long diff = operand1 - operand2;
        result[0] = diff - borrow;
        // diff > a must produce borrow
        // diff < borrow, only in this case: borrow = 1, diff = 0, will produce new borrow
        return (Long.compareUnsigned(diff, operand1) > 0 || Long.compareUnsigned(diff, borrow) < 0) ? 1 : 0;
    }

    /**
     * Computes (operand1 - operand2) -> (diff, borrow), diff is stored in result[0, uint64Count), borrow is returned.
     *
     * @param operand1    operand1.
     * @param uint64Count number of uint64 in operand1.
     * @param operand2    an unsigned 64-bit operand2.
     * @param result      result[0, uint64Count) stores the (operand1 - operand2).
     * @return (operand1 - operand2)'s borrow (0 or 1).
     */
    public static long subUint(long[] operand1, int uint64Count, long operand2, long[] result) {
        assert uint64Count > 0;
        long[] tmp = new long[1];
        long borrow;
        borrow = subUint64(operand1[0], operand2, tmp);
        result[0] = tmp[0];

        int i = 1;
        while (--uint64Count > 0) {
            borrow = subUint64(operand1[i], 0, borrow, tmp);
            result[i] = tmp[0];
            i++;
        }
        return borrow;
    }

    /**
     * Computes (operand1 - operand2) -> (diff, borrow), diff is stored in result[0, uint64Count), borrow is returned.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, uint64Count) stores the (operand1 - operand2).
     * @return (operand1 - operand2)'s borrow (0 or 1).
     */
    public static long subUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        assert uint64Count > 0;
        long[] tmp = new long[1];
        long borrow = subUint64(operand1[0], operand2[0], tmp);
        result[0] = tmp[0];
        int i = 1;
        while (--uint64Count > 0) {
            borrow = subUint64(operand1[i], operand2[i], borrow, tmp);
            result[i] = tmp[0];
            i++;
        }
        return borrow;
    }

    /**
     * Computes (operand1 - operand2) -> (diff, borrow), diff is stored in result[0, uint64Count), borrow is returned.
     *
     * @param operand1    operand1.
     * @param pos1        position of operand1.
     * @param operand2    operand2.
     * @param pos2        position of operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, uint64Count) stores the (operand1 - operand2).
     * @return (operand1 - operand2)'s borrow (0 or 1).
     */
    public static long subUint(long[] operand1, int pos1, long[] operand2, int pos2, int uint64Count, long[] result) {
        assert uint64Count > 0;
        long[] tmp = new long[1];
        long borrow = subUint64(operand1[pos1], operand2[pos2], tmp);
        result[0] = tmp[0];
        int i = 1;
        while (--uint64Count > 0) {
            borrow = subUint64(operand1[pos1 + i], operand2[pos2 + i], borrow, tmp);
            result[i] = tmp[0];
            i++;
        }
        return borrow;
    }

    /**
     * Computes (operand1 - operand2) -> (diff, borrow), diff is stored in result[0, resultUint64Count), borrow is returned.
     *
     * @param operand1            operand1.
     * @param operand1Uint64Count number of uint64 in operand1.
     * @param operand2            operand2.
     * @param operand2Uint64Count number of uint64 in operand2.
     * @param borrow              the given borrow (0 or 1).
     * @param resultUint64Count   number of uint64 in result.
     * @param result              result[0, resultUint64Count) stores the (operand1 - operand2).
     * @return (operand1 - operand2)'s borrow (0 or 1).
     */
    public static long subUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count,
                               long borrow, int resultUint64Count, long[] result) {
        assert resultUint64Count > 0;
        long[] tmp = new long[1];
        for (int i = 0; i < resultUint64Count; i++) {
            borrow = subUint64(
                i < operand1Uint64Count ? operand1[i] : 0,
                i < operand2Uint64Count ? operand2[i] : 0,
                borrow,
                tmp
            );
            result[i] = tmp[0];
        }
        return borrow;
    }

    /**
     * Computes (operand1 + operand2) -> (sum, carry), sum is stored in result[0], carry is returned.
     *
     * @param operand1 an unsigned 64-bit operand1.
     * @param operand2 an unsigned 64-bit operand2.
     * @param result   result[0] stores the lower unsigned 64-bit (operand1 + operand2).
     * @return (operand1 + operand2)'s carry (0 or 1).
     */
    public static long addUint64(long operand1, long operand2, long[] result) {
        // we do not care result.length, just directly stores the result in result[0]
        result[0] = operand1 + operand2;
        return Long.compareUnsigned(result[0], operand1) < 0 ? 1 : 0;
    }

    /**
     * Computes (operand1 + operand2 + carry) -> (sum, carry), sum is stored in result[0], carry is returned.
     *
     * @param operand1 an unsigned 64-bit operand1.
     * @param operand2 an unsigned 64-bit operand2.
     * @param carry    the given carry (0 or 1).
     * @param result   result[0] stores the lower unsigned 64-bit (operand1 + operand2 + carry).
     * @return (operand1 + operand2 + carry)'s carry (0 or 1).
     */
    public static long addUint64(long operand1, long operand2, long carry, long[] result) {
        return addUint64Generic(operand1, operand2, carry, result);
    }

    /**
     * Computes (operand1 + operand2 + carry) -> (sum, carry), sum is stored in result[0], carry is returned.
     *
     * @param operand1 an unsigned 64-bit operand1.
     * @param operand2 an unsigned 64-bit operand2.
     * @param carry    the given carry (0 or 1).
     * @param result   result[0] stores the lower unsigned 64-bit (operand1 + operand2 + carry).
     * @return (operand1 + operand2 + carry)'s carry (0 or 1).
     */
    public static long addUint64Generic(long operand1, long operand2, long carry, long[] result) {
        long sum = operand1 + operand2;
        result[0] = sum + carry;
        boolean isCarry = Long.compareUnsigned(sum, operand1) < 0 || (sum == -1 && carry == 1);
        return isCarry ? 1 : 0;
    }

    /**
     * Computes (operand1 + operand2) -> (sum, carry), sum store in result[0, uint64Count), carry is returned.
     *
     * @param operand1    operand1.
     * @param uint64Count number of uint64 in operand1.
     * @param operand2    an unsigned 64-bit operand2.
     * @param result      result[0, uint64Count) stores the result.
     * @return (operand1 + operand2)'s carry (0 or 1).
     */
    public static long addUint(long[] operand1, int uint64Count, long operand2, long[] result) {
        return addUint(operand1, 0, uint64Count, operand2, result, 0);
    }

    /**
     * Computes (operand1 + operand2) -> (sum, carry), sum store in result[0, uint64Count), carry is returned.
     *
     * @param operand1    operand1.
     * @param pos1        operand1 position.
     * @param uint64Count number of uint64 in operand1.
     * @param operand2    an unsigned 64-bit operand2.
     * @param result      result[0, uint64Count) stores the result.
     * @param posR        result position.
     * @return (operand1 + operand2)'s carry (0 or 1).
     */
    public static long addUint(long[] operand1, int pos1, int uint64Count, long operand2, long[] result, int posR) {
        assert uint64Count > 0;
        long[] tmp = new long[1];
        long carry = addUint64(operand1[pos1], operand2, tmp);
        result[posR] = tmp[0];
        int i = 1;
        while (--uint64Count > 0) {
            carry = addUint64(operand1[pos1 + i], 0, carry, tmp);
            result[posR + i] = tmp[0];
            i++;
        }
        return carry;
    }

    /**
     * Computes (operand1 + operand2) -> (sum, carry), sum is stored in result[0, uint64Count), carry is returned.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, uint64Count) stores the result.
     * @return (operand1 + operand2)'s carry (0 or 1).
     */
    public static long addUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        assert uint64Count > 0;
        long[] tmp = new long[1];
        long carry;
        carry = addUint64(operand1[0], operand2[0], tmp);
        result[0] = tmp[0];

        int i = 1;
        while (--uint64Count > 0) {
            carry = addUint64(operand1[i], operand2[i], carry, tmp);
            result[i] = tmp[0];
            i++;
        }
        return carry;
    }

    /**
     * Computes (operand1 + operand2 + corry) -> (sum, carry), sum is stored in result[0, resultUint64Count), carry is returned.
     *
     * @param operand1            operand1.
     * @param operand1Uint64Count number of uint64 in operand1.
     * @param operand2            operand2.
     * @param operand2Uint64Count number of uint64 in operand2.
     * @param carry               the given carry (0 or 1).
     * @param resultUint64Count   number of uint64 in result.
     * @param result              result[0, resultUint64Count) store the result.
     * @return (operand1 + operand2 + carry)'s carry (0 or 1).
     */
    public static long addUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count,
                               long carry, int resultUint64Count, long[] result) {
        assert resultUint64Count > 0;
        long[] tmp = new long[1];
        for (int i = 0; i < resultUint64Count; i++) {
            carry = addUint64(
                i < operand1Uint64Count ? operand1[i] : 0,
                i < operand2Uint64Count ? operand2[i] : 0,
                carry,
                tmp
            );
            result[i] = tmp[0];
        }
        return carry;
    }

    /**
     * compute operand ^ exponent.
     *
     * @param operand  an unsigned 64-bit operand.
     * @param exponent an exponent.
     * @return operand ^ exponent.
     */
    public static long exponentUint(long operand, long exponent) {
        if (operand == 0) {
            return 0;
        }
        if (exponent == 0) {
            return 1;
        }
        if (operand == 1) {
            return 1;
        }
        // Perform binary exponentiation.
        long power = operand;
        long product;
        long intermediate = 1;

        // Initially: power = operand and intermediate = 1, product irrelevant.
        while (true) {
            if ((exponent & 1) > 0) {
                product = power * intermediate;
                // swap
                intermediate = product;
            }
            exponent >>>= 1;
            if (exponent == 0) {
                break;
            }
            product = power * power;
            // swap
            power = product;
        }
        return intermediate;
    }

}
