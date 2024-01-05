package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;

/**
 * Uint arithmetic under a large modulus. A large modulus means that the modulus is represented by a long array.
 * When reconstruct CRT, we need q = q1 * q2 * ... * qn, we need long[] to represent the q
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/uintarithmod.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/5
 */
public class UintArithmeticMod {
    /**
     * private constructor.
     */
    private UintArithmeticMod() {
        // empty
    }

    /**
     * Computes (operand + 1) mod modulus and store it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param modulus     modulus.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores (operand + 1) % modulus.
     */
    public static void incrementUintMod(long[] operand, long[] modulus, int uint64Count, long[] result) {
        assert operand != null;
        assert modulus != null;
        assert uint64Count > 0;
        assert result != null;
        // operand < modulus
        assert UintCore.isLessThanUint(operand, modulus, uint64Count);
        // two different array
        assert modulus != result;
        // operand + 1
        long carry = UintArithmetic.incrementUint(operand, uint64Count, result);
        // reduce
        if (carry > 0 || UintCore.isGreaterThanOrEqualUint(result, modulus, uint64Count)) {
            UintArithmetic.subUint(result, modulus, uint64Count, result);
        }
    }

    /**
     * Computes (operand - 1) mod modulus and store it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param modulus     modulus.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores (operand - 1) mod modulus.
     */
    public static void decrementUintMod(long[] operand, long[] modulus, int uint64Count, long[] result) {
        assert operand != null;
        assert modulus != null;
        assert result != null;
        assert uint64Count > 0;
        // operand < modulus
        assert UintCore.isLessThanUint(operand, modulus, uint64Count);
        // two different array
        assert modulus != result;
        // operand - 1
        long borrow = UintArithmetic.decrementUint(operand, uint64Count, result);
        if (borrow > 0) {
            UintArithmetic.addUint(result, modulus, uint64Count, result);
        }
    }

    /**
     * Computes (-operand mod modulus) and store it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param modulus     modulus.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores (-operand mod modulus).
     */
    public static void negateUintMod(long[] operand, long[] modulus, int uint64Count, long[] result) {
        assert operand != null;
        assert modulus != null;
        assert result != null;
        assert uint64Count > 0;
        // operand < modulus
        assert UintCore.isLessThanUint(operand, modulus, uint64Count);
        // two different array
        assert modulus != result;
        // operand - 1
        if (UintCore.isZeroUint(operand, uint64Count)) {
            UintCore.setZeroUint(uint64Count, result);
        } else {
            // Otherwise, we know operand > 0 and < modulus so subtract modulus - operand.
            UintArithmetic.subUint(modulus, operand, uint64Count, result);
        }
    }

    /**
     * Computes (operand / 2 mod modulus) and store it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param modulus     modulus.
     * @param uint64Count number of uint64 in operand.
     * @param result      result[0, uint64Count) stores (operand / 2 mod modulus).
     */
    public static void div2UintMod(long[] operand, long[] modulus, int uint64Count, long[] result) {
        assert operand != null;
        assert modulus != null;
        assert result != null;
        assert uint64Count > 0;
        // the modulus must be an odd number, otherwise we cannot find inv(2).
        assert UintCore.isBitSetUint(modulus, uint64Count, 0);
        // operand < modulus, so we do not need to handle the mod operation
        assert UintCore.isLessThanUint(operand, modulus, uint64Count);

        if ((operand[0] & 1) > 0) {
            // odd
            long carry = UintArithmetic.addUint(operand, modulus, uint64Count, result);
            UintArithmetic.rightShiftUint(result, 1, uint64Count, result);
            if (carry > 0) {
                UintCore.setBitUint(result, uint64Count, uint64Count * Constants.UINT64_BITS - 1);
            }
        } else {
            // even
            UintArithmetic.rightShiftUint(operand, 1, uint64Count, result);
        }
    }

    /**
     * Computes (operand1 + operand1) mod modulus and stores it in result[0, uint64Count).
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param modulus     modulus.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, uint64Count) stores (operand1 + operand1) mod modulus.
     */
    public static void addUintUintMod(long[] operand1, long[] operand2, long[] modulus, int uint64Count, long[] result) {
        assert operand1 != null;
        assert operand2 != null;
        assert modulus != null;
        assert uint64Count > 0;
        assert UintCore.isLessThanUint(operand1, modulus, uint64Count);
        assert UintCore.isLessThanUint(operand2, modulus, uint64Count);
        assert result != modulus;

        long carry = UintArithmetic.addUint(operand1, operand2, uint64Count, result);
        if (carry > 0 || UintCore.isGreaterThanOrEqualUint(result, modulus, uint64Count)) {
            UintArithmetic.subUint(result, modulus, uint64Count, result);
        }
    }

    /**
     * Computes (operand1 - operand1) mod modulus and stores it in result[0, uint64Count).
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param modulus     modulus.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @param result      result[0, uint64Count) stores (operand1 - operand1) mod modulus.
     */
    public static void subUintUintMod(long[] operand1, long[] operand2, long[] modulus, int uint64Count, long[] result) {
        assert operand1 != null;
        assert operand2 != null;
        assert modulus != null;
        assert uint64Count > 0;
        assert UintCore.isLessThanUint(operand1, modulus, uint64Count);
        assert UintCore.isLessThanUint(operand2, modulus, uint64Count);
        assert result != modulus;

        long borrow = UintArithmetic.subUint(operand1, operand2, uint64Count, result);
        if (borrow > 0) {
            UintArithmetic.addUint(result, modulus, uint64Count, result);
        }
    }


    /**
     * Computes (operand^{-1}) mod modulus and stores it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param modulus     modulus.
     * @param uint64Count umber of uint64 in operand.
     * @param result      result[0, uint64Count) stores (operand^{-1}) mod modulus.
     * @return true if operand's invert exist; otherwise, return false
     */
    public static boolean tryInvertUintMod(long[] operand, long[] modulus, int uint64Count, long[] result) {
        assert operand != null;
        assert modulus != null;
        assert uint64Count > 0;
        // operand < modulus
        assert UintCore.isLessThanUint(operand, modulus, uint64Count);
        // invert 0
        int bitCount = UintCore.getSignificantBitCountUint(operand, uint64Count);
        if (bitCount == 0) {
            return false;
        }
        // invert 1, result = 1
        if (bitCount == 1) {
            UintCore.setUint(1, uint64Count, result);
            return true;
        }

        long[] numerator = new long[uint64Count];
        UintCore.setUint(modulus, uint64Count, numerator);
        long[] denominator = new long[uint64Count];
        UintCore.setUint(operand, uint64Count, denominator);
        long[] difference = new long[uint64Count];
        int numeratorBits = UintCore.getSignificantBitCountUint(numerator, uint64Count);
        int denominatorBits = UintCore.getSignificantBitCountUint(denominator, uint64Count);
        long[] quotient = new long[uint64Count];

        // Create three sign/magnitude values to store coefficients.
        // Initialize invert_prior to +0 and invert_curr to +1.
        long[] invertPrior = new long[uint64Count];
        boolean invertPriorPositive = true;

        long[] invertCurr = new long[uint64Count];
        UintCore.setUint(1, uint64Count, invertCurr);
        boolean invertCurrPositive = true;

        long[] invertNext = new long[uint64Count];
        boolean invertNextPositive = true;
        // Perform extended Euclidean algorithm.
        while (true) {
            // NOTE: Numerator is > denominator.
            // Only perform computation up to last non-zero uint64s.
            int divisionUint64Count = UintCore.divideRoundUp(numeratorBits, UintArithmetic.UINT64_BITS);
            // Shift denominator to bring MSB in alignment with MSB of numerator.
            int denominatorShift = numeratorBits - denominatorBits;
            UintArithmetic.leftShiftUint(denominator, denominatorShift, divisionUint64Count, denominator);
            denominatorBits += denominatorShift;

            // clear quotient
            UintCore.setZeroUint(uint64Count, quotient);
            // Perform bit-wise division algorithm.
            int remainingShifts = denominatorShift;
            while (numeratorBits == denominatorBits) {
                // NOTE: MSBs of numerator and denominator are aligned.

                // Even though MSB of numerator and denominator are aligned, still possible numerator < denominator.
                long borrow = UintArithmetic.subUint(numerator, denominator, divisionUint64Count, difference);
                if (borrow > 0) {
                    // numerator < shifted_denominator and MSBs are aligned,
                    // so current quotient bit is zero and next one is definitely one.
                    if (remainingShifts == 0) {
                        // No shifts remain and numerator < denominator so done.
                        break;
                    }
                    // Effectively shift numerator left by 1 by instead adding
                    // numerator to difference (to prevent overflow in numerator).
                    UintArithmetic.addUint(difference, numerator, divisionUint64Count, difference);
                    // Adjust quotient and remaining shifts as a result of
                    // shifting numerator.
                    UintArithmetic.leftShiftUint(quotient, 1, divisionUint64Count, quotient);
                    remainingShifts--;
                }
                // Difference is the new numerator with denominator subtracted.
                // Update quotient to reflect subtraction.
                quotient[0] |= 1;
                // Determine amount to shift numerator to bring MSB in alignment with denominator.
                numeratorBits = UintCore.getSignificantBitCountUint(difference, divisionUint64Count);
                int numeratorShift = denominatorBits - numeratorBits;
                if (numeratorShift > remainingShifts) {
                    // Clip the maximum shift to determine only the integer (as opposed to fractional) bits.
                    numeratorShift = remainingShifts;
                }
                // Shift and update numerator.
                if (numeratorBits > 0) {
                    UintArithmetic.leftShiftUint(difference, numeratorShift, divisionUint64Count, numerator);
                    numeratorBits += numeratorShift;
                } else {
                    // if numeratorBits = 0, mean difference = 0, so numerator = 0
                    UintCore.setZeroUint(divisionUint64Count, numerator);
                }
                // Adjust quotient and remaining shifts as a result of shifting numerator.
                UintArithmetic.leftShiftUint(quotient, numeratorShift, divisionUint64Count, quotient);
                remainingShifts -= numeratorShift;
            }
            // Correct for shifting of denominator.
            UintArithmetic.rightShiftUint(denominator, denominatorShift, divisionUint64Count, denominator);
            denominatorBits -= denominatorShift;
            // We are done if remainder (which is stored in numerator) is zero.
            if (numeratorBits == 0) {
                break;
            }
            // Correct for shifting of denominator.
            UintArithmetic.rightShiftUint(numerator, denominatorShift, divisionUint64Count, numerator);
            numeratorBits -= denominatorShift;
            // Integrate quotient with invert coefficients. Calculate: invert_prior + -quotient * invert_curr
            UintArithmetic.multiplyTruncateUint(quotient, invertCurr, uint64Count, invertNext);
            invertNextPositive = !invertCurrPositive;
            if (invertPriorPositive == invertNextPositive) {
                // If both sides of add have same sign, then simply add and
                // do not need to worry about overflow due to known limits
                // on the coefficients proved in the euclidean algorithm.
                UintArithmetic.addUint(invertPrior, invertNext, uint64Count, invertNext);
            } else {
                // If both sides of add have opposite sign, then subtract and check for overflow.
                long borrow = UintArithmetic.subUint(invertPrior, invertNext, uint64Count, invertNext);
                if (borrow == 0) {
                    // No borrow means |invert_prior| >= |invert_next|,
                    // so sign is same as invert_prior.
                    invertNextPositive = invertPriorPositive;
                } else {
                    // Borrow means |invert prior| < |invert_next|, so sign is opposite of invert_prior.
                    invertNextPositive = !invertPriorPositive;
                    UintArithmetic.negateUint(invertNext, uint64Count, invertNext);
                }
            }
            // Swap prior and curr, and then curr and next.
            swap(invertPrior, invertCurr);
            boolean tmp = invertPriorPositive;
            invertPriorPositive = invertCurrPositive;
            invertCurrPositive = tmp;

            swap(invertCurr, invertNext);
            boolean tmp2 = invertCurrPositive;
            invertCurrPositive = invertNextPositive;
            invertNextPositive = tmp2;

            // Swap numerator and denominator using pointer swings.
            swap(numerator, denominator);
            int tmp3 = numeratorBits;
            numeratorBits = denominatorBits;
            denominatorBits = tmp3;
        }
        if (!UintCore.isEqualUint(denominator, uint64Count, 1)) {
            // GCD is not one, so unable to find inverse.
            return false;
        }

        // Correct coefficient if negative by modulo.
        if (!invertCurrPositive && !UintCore.isZeroUint(invertCurr, uint64Count)) {
            UintArithmetic.subUint(modulus, invertCurr, uint64Count, invertCurr);
            invertCurrPositive = true;
        }
        // set result
        UintCore.setUint(invertCurr, uint64Count, result);
        return true;
    }

    /**
     * swap operand1 and operand2 with deep copy.
     *
     * @param operand1 operand1.
     * @param operand2 operand2.
     */
    private static void swap(long[] operand1, long[] operand2) {
        assert operand1.length == operand2.length;
        long[] tmp = new long[operand1.length];
        // tmp = a
        System.arraycopy(operand1, 0, tmp, 0, operand1.length);
        // a = b
        System.arraycopy(operand2, 0, operand1, 0, operand1.length);
        // b = tmp
        System.arraycopy(tmp, 0, operand2, 0, operand2.length);
    }

}
