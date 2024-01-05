package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;

import java.util.Arrays;

/**
 * uint (base-2^64 value) core operations. All base-2^64 values are represented in little-endian, that is,
 * given x = [x_0, x_1, x_2, ...], we can think of it as representing x_0 + 2^64 * x_1 + 2^128 * x_2 + ... .
 * We can think of it as an in-place BigInteger implementation.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/uintcore.cpp
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/4
 */
public class UintCore {
    /**
     * private constructor.
     */
    private UintCore() {
        // empty
    }

    /**
     * Converts an uint value to a decimal string.
     *
     * @param value       an uint value.
     * @param uint64Count number of uint64 in the value.
     * @return a decimal string.
     */
    public static String uintToDecimalString(long[] value, int uint64Count) {
        assert !(uint64Count > 0 && value == null);
        if (uint64Count == 0) {
            return "0";
        }
        // the basic idea is to iteratively divide by 10 to get quotient and remainder, and keep remainders.
        // assume value = 1235, then
        // 1st round: 1235 --div 10--> remainder = 5, quotient = 123
        // 2nd round: 123  --div 10--> remainder = 3, quotient = 12
        // 3rd round: 12   --div 10--> remainder = 2, quotient = 1
        // 4th round: 1    --div 10--> remainder = 1, quotient = 0
        // finally flip (5321) to get (1235)
        long[] remainder = new long[uint64Count];
        long[] quotient = new long[uint64Count];
        long[] base = new long[uint64Count];
        // base = [10, 10, ..., 10]
        setUint(10, uint64Count, base);
        // remainder = value
        setUint(value, uint64Count, remainder);
        StringBuilder output = new StringBuilder();
        while (!isZeroUint(remainder, uint64Count)) {
            // value / 10 = [quotient, remainder]
            UintArithmetic.divideUintInplace(remainder, base, uint64Count, quotient);
            // put the remainder into the StringBuilder
            char digit = (char) ((char) remainder[0] + '0');
            output.append(digit);
            // update quotient into remainder for the next division
            System.arraycopy(quotient, 0, remainder, 0, uint64Count);
        }
        // flip output
        output.reverse();
        String result = output.toString();
        if (result.isEmpty()) {
            return "0";
        }
        return result;
    }

    /**
     * Convert an uint value to a hex string.
     *
     * @param value       an uint value.
     * @param uint64Count number of uint64 in the value.
     * @return a hex string.
     */
    public static String uintToHexString(long[] value, int uint64Count) {
        return uintToHexString(value, 0, uint64Count);
    }

    /**
     * Converts an uint value to a hex String, where the value is in [startIndex, startIndex + uint64Count).
     *
     * @param value       an value.
     * @param startIndex  the start index of the value.
     * @param uint64Count number of uint64 in the value.
     * @return a hex string.
     */
    public static String uintToHexString(long[] value, int startIndex, int uint64Count) {
        assert !(uint64Count > 0 && value == null);
        // Start with a string with a zero for each nibble in the array.
        // nibble is a 4-bit decimal value in range [0, 16)
        int numNibbles = Common.mulSafe(uint64Count, Common.NIBBLES_PER_UINT64, false);
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < numNibbles; i++) {
            output.append('0');
        }
        // Iterate through each uint64 in array and set string with correct nibbles in hex.
        int nibbleIndex = numNibbles;
        int leftMostNonZeroPos = numNibbles;
        for (int i = 0; i < uint64Count; i++) {
            long part = value[startIndex + i];
            // Iterate through each nibble in the current uint64.
            for (int j = 0; j < Common.NIBBLES_PER_UINT64; j++) {
                int nibble = (int) (part & (long) 0x0F);
                int pos = --nibbleIndex;
                if (nibble != 0) {
                    // If nibble is not zero, then update string and save this pos to determine number of leading zeros.
                    output.setCharAt(pos, Common.nibbleToUpperHex(nibble));
                    leftMostNonZeroPos = pos;
                }
                // right-shift 4-bit, handle next nibble
                part >>>= 4;
            }
        }
        // Trim string to remove leading zeros.
        String result = output.substring(leftMostNonZeroPos);
        if (result.isEmpty()) {
            return "0";
        }
        return result;
    }


    /**
     * Converts a hex string to an uint value, and writes the value in result.
     *
     * @param hexString   a hex string.
     * @param charCount   number of chars in the hex string.
     * @param uint64Count number of uint64 in the value.
     * @param result      result to write the value.
     */
    public static void hexStringToUint(String hexString, int charCount, int uint64Count, long[] result) {
        hexStringToUint(hexString, 0, charCount, uint64Count, 0, result);
    }

    /**
     * Converts a hex string to an uint value, and writes the value in result, where the hex string is in [startIndex,
     * startIndex + charCount), and the value is in result[resultStartIndex, resultStartIndex + uint64Count).
     *
     * @param hexString        a hex string.
     * @param startIndex       start index of the hex string.
     * @param charCount        number of chars in the hex string.
     * @param uint64Count      number of uint64 in the value.
     * @param resultStartIndex start index of the value.
     * @param result           result to write the value.
     */
    public static void hexStringToUint(String hexString, int startIndex, int charCount, int uint64Count, int resultStartIndex, long[] result) {
        assert !(hexString == null && charCount > 0);
        // we cannot put assert hexString != null here, since there is a case that hexString == null and charCount == 0
        assert !(uint64Count > 0 && result == null);
        assert !Common.unsignedGt(Common.getHexStringBitCount(hexString, charCount), Common.mulSafe(uint64Count, Common.BITS_PER_UINT64, true));
        // start with the last hex char
        int hexStringIndex = charCount + startIndex;
        for (int uint64Index = 0; uint64Index < uint64Count; uint64Index++) {
            long value = 0;
            // handle each nibble
            for (int bitIndex = 0; bitIndex < Common.BITS_PER_UINT64; bitIndex += Common.BITS_PER_NIBBLE) {
                if (hexStringIndex == startIndex) {
                    break;
                }
                assert hexString != null;
                char hex = hexString.charAt(--hexStringIndex);
                int nibble = Common.hexToNibble(hex);
                if (nibble == -1) {
                    throw new IllegalArgumentException("current char: " + hex + "is not a hex char");
                }
                value |= ((long) nibble << bitIndex);
            }
            result[resultStartIndex + uint64Index] = value;
        }
    }

    /**
     * Returns k where 2^k = n if n is a power of 2. Returns -1 otherwise.
     *
     * @param value value n.
     * @return k where 2^k = n.
     */
    public static int getPowerOfTwo(long value) {
        if (value == 0 || (value & (value - 1)) != 0) {
            return -1;
        }
        return Common.getMsbIndex(value);
    }

    /**
     * Returns if an uint value = 0.
     *
     * @param value       an uint value.
     * @param uint64Count number of uint64 in the value.
     * @return true if value = 0.
     */
    public static boolean isZeroUint(long[] value, int uint64Count) {
        return isZeroUint(value, 0, uint64Count);
    }

    /**
     * Returns if an uint value = 0, where the value is in value[startIndex, startIndex + uint64Count).
     *
     * @param value       an uint value.
     * @param startIndex  start index of the value.
     * @param uint64Count number of uint64 in the value.
     * @return true if value = 0.
     */
    public static boolean isZeroUint(long[] value, int startIndex, int uint64Count) {
        assert uint64Count > 0;
        for (int i = startIndex; i < startIndex + uint64Count; i++) {
            if (value[i] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns if an uint value = scalar.
     *
     * @param value       an uint value.
     * @param uint64Count number of uint64 in the value.
     * @param scalar      a scalar.
     * @return true if value = scalar.
     */
    public static boolean isEqualUint(long[] value, int uint64Count, long scalar) {
        assert uint64Count > 0;
        if (value[0] != scalar) {
            return false;
        }
        // if value[0] == scalar, then only if value[1..] all equal 0, then value = scalar, return true
        return Arrays.stream(value, 1, uint64Count).allMatch(n -> n == 0);
    }

    /**
     * Returns if two uint values operand1 == operand2.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @return true if operand1 == operand2.
     */
    public static boolean isEqualUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, operand2, uint64Count) == 0;
    }

    /**
     * Returns if two uint values operand1 > operand2.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @return true if operand1 > operand2.
     */
    public static boolean isGreaterThanUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, operand2, uint64Count) > 0;
    }

    /**
     * Returns if two uint values operand1 >= operand2.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @return true if operand1 >= operand2.
     */
    public static boolean isGreaterThanOrEqualUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, operand2, uint64Count) >= 0;
    }

    /**
     * Returns if two uint values operand1 >= operand2.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @return true if operand1 >= operand2.
     */
    public static boolean isGreaterThanOrEqualUint(long[] operand1, int pos1, long[] operand2, int pos2, int uint64Count) {
        return compareUint(operand1, pos1, operand2, pos2, uint64Count) >= 0;
    }

    /**
     * Returns if two uint values operand1 < operand2.
     *
     * @param operand1            operand1.
     * @param operand1Uint64Count number of uint64 in operand1.
     * @param operand2            operand2.
     * @param operand2Uint64Count number of uint64 in operand2.
     * @return true if operand1 < operand2.
     */
    public static boolean isLessThanUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {
        return compareUint(operand1, operand1Uint64Count, operand2, operand2Uint64Count) < 0;
    }

    /**
     * Returns if two uint values operand1 < operand2.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @return true if operand1 < operand2.
     */
    public static boolean isLessThanUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, operand2, uint64Count) < 0;
    }

    /**
     * Returns if two uint values operand1 <= operand2.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @return true if operand1 <= operand2.
     */
    public static boolean isLessThanOrEqualUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, operand2, uint64Count) <= 0;
    }

    /**
     * Compares operand1 and operand2.
     *
     * @param operand1            operand1.
     * @param operand1Uint64Count number of uint64 used in operand1.
     * @param operand2            operand2.
     * @param operand2Uint64Count number of uint64 used in operand2.
     * @return if operand1 > operand2 then return 1, else if operand1 < operand2 return 01, otherwise return 0.
     */
    public static int compareUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {
        assert operand1Uint64Count > 0 && operand2Uint64Count > 0;
        int result = 0;
        int operand1Index = operand1Uint64Count - 1;
        int operand2Index = operand2Uint64Count - 1;
        int minUint64Count = Math.min(operand1Uint64Count, operand2Uint64Count);
        // 0 or > 0
        operand1Uint64Count -= minUint64Count;
        for (; (result == 0) && operand1Uint64Count-- > 0; operand1Index--) {
            // once != 0, operand1 > operand2
            result = operand1[operand1Index] != 0 ? 1 : 0;
        }
        operand2Uint64Count -= minUint64Count;
        for (; (result == 0) && operand2Uint64Count-- > 0; operand2Index--) {
            result = -(operand1[operand1Index] != 0 ? 1 : 0);
        }
        for (; result == 0 && minUint64Count-- > 0; operand1Index--, operand2Index--) {
            result = Long.compareUnsigned(operand1[operand1Index], operand2[operand2Index]);
        }
        return result;
    }

    /**
     * Compares operand1 and operand2.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @return if operand1 > operand2 then return 1, else if operand1 < operand2 return 01, otherwise return 0.
     */
    public static int compareUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, 0, operand2, 0, uint64Count);
    }

    /**
     * Compares operand1 and operand2.
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of uint64 in operand1 and operand2.
     * @return if operand1 > operand2 then return 1, else if operand1 < operand2 return 01, otherwise return 0.
     */
    public static int compareUint(long[] operand1, int pos1, long[] operand2, int pos2, int uint64Count) {
        assert uint64Count > 0;
        int result = 0;
        int index = uint64Count - 1;
        // once result = 1, break loop
        for (; result == 0 && uint64Count-- > 0; index--) {
            result = Long.compareUnsigned(operand1[pos1 + index], operand2[pos2 + index]);
        }
        return result;
    }

    /**
     * Gets the most significant uint64 count for the value. For example, given value = [1, 0, 2] and uint64Count = 2
     * (which means that we consider value as [1, 0]), it returns 1 since the significant Uint64Count for [1, 0] is 1.
     *
     * @param value       the value.
     * @param uint64Count number of uint64 in the value.
     * @return the most significant uint64 count for the value.
     */
    public static int getSignificantUint64CountUint(long[] value, int uint64Count) {
        assert uint64Count > 0;
        assert value.length >= uint64Count;
        int index = uint64Count - 1;
        // from right to left and check if value[index] != 0.
        for (; uint64Count > 0 && value[index] == 0; uint64Count--) {
            index--;
        }
        return uint64Count;
    }

    /**
     * Gets the number of non-zero uint64 in the value.
     *
     * @param value       the value.
     * @param uint64Count number of uint64 in the value.
     * @return the number of non-zero uint64 in the value.
     */
    public static int getNonZeroUint64CountUint(long[] value, int uint64Count) {
        assert uint64Count > 0;
        int nonZeroCount = uint64Count;
        int index = uint64Count - 1;
        for (; uint64Count > 0; uint64Count--) {
            if (value[index] == 0) {
                nonZeroCount--;
            }
            index--;
        }
        return nonZeroCount;
    }


    /**
     * Gets the most significant bit-count in the value. For example:
     * <li>[0, 0, 1] ---> 63 + 63 + 1 = 127 bits</li>
     * <li>[1, 0, 0] --->  1 + 0 + 0 = 1 bits</li>
     *
     * @param value       the value.
     * @param uint64Count number of uint64 in the value.
     * @return the most significant bit-count in the value.
     */
    public static int getSignificantBitCountUint(long[] value, int uint64Count) {
        assert uint64Count <= value.length;
        int index = uint64Count - 1;
        for (; value[index] == 0 && uint64Count > 1; uint64Count--) {
            index--;
        }
        return (uint64Count - 1) * 64 + getSignificantBitCount(value[index]);
    }

    /**
     * Gets the most significant bit-count in the value.
     *
     * @param value the value.
     * @return the most significant bit-count in the value.
     */
    public static int getSignificantBitCount(long value) {
        return 64 - Long.numberOfLeadingZeros(value);
    }

    /**
     * Computes ⌈value / divisor⌉, i.e., the smallest integer greater than or equal to value / divisor.
     *
     * @param value   the value.
     * @param divisor the divisor.
     * @return ⌈value / divisor⌉.
     */
    public static int divideRoundUp(int value, int divisor) {
        assert value >= 0;
        assert divisor > 0;
        return (value + divisor - 1) / divisor;
    }

    /**
     * Sets value = 0.
     *
     * @param uint64Count number of uint64 in the value.
     * @param value       the value.
     */
    public static void setZeroUint(int uint64Count, long[] value) {
        setZeroUint(uint64Count, value, 0);
    }

    /**
     * Sets value = 0.
     *
     * @param uint64Count number of uint64 in the value.
     * @param value       the value.
     * @param pos         the start position.
     */
    public static void setZeroUint(int uint64Count, long[] value, int pos) {
        Arrays.fill(value, pos, pos + uint64Count, 0);
    }

    /**
     * Sets result = scalar.
     *
     * @param scalar      the scalar.
     * @param uint64Count number of uint64 in the value.
     * @param result      the result to write the value.
     */
    public static void setUint(long scalar, int uint64Count, long[] result) {
        assert uint64Count > 0;
        assert result.length >= uint64Count;
        result[0] = scalar;
        for (int i = 1; i < uint64Count; i++) {
            result[i] = 0;
        }
    }

    /**
     * Sets result = value.
     *
     * @param value       the value.
     * @param uint64Count number of uint64 in the value and the result.
     * @param result      the result to write the value.
     */
    public static void setUint(long[] value, int uint64Count, long[] result) {
        assert uint64Count >= 0;
        if (result == value || uint64Count == 0) {
            return;
        }
        System.arraycopy(value, 0, result, 0, uint64Count);
    }

    /**
     * Sets value = result.
     *
     * @param value             the value.
     * @param valueUint64Count  number of uint64 in the value.
     * @param result            the result to write the value.
     * @param resultUint64Count number of uint64 in the result.
     */
    public static void setUint(long[] value, int valueUint64Count, long[] result, int resultUint64Count) {
        setUint(value, 0, valueUint64Count, result, 0, resultUint64Count);
    }

    /**
     * Sets value = result.
     *
     * @param value             the value.
     * @param pos               the start position.
     * @param valueUint64Count  number of uint64 in the value.
     * @param result            the result to write the value.
     * @param posR              the result start position.
     * @param resultUint64Count number of uint64 in the result.
     */
    public static void setUint(long[] value, int pos, int valueUint64Count, long[] result, int posR, int resultUint64Count) {
        assert value.length >= pos + valueUint64Count;
        assert result.length >= posR + resultUint64Count;
        if (value == result || valueUint64Count == 0) {
            Arrays.fill(result, posR + valueUint64Count, posR + resultUint64Count, 0);
        } else {
            int minUint64Count = Math.min(valueUint64Count, resultUint64Count);
            System.arraycopy(value, pos, result, posR, minUint64Count);
            Arrays.fill(result, posR + minUint64Count, posR + resultUint64Count, 0);
        }
    }

    /**
     * Sets the specific bitIndex of value to 1, where bitIndex = 0 means the left-most bit in the value.
     *
     * @param value       the value.
     * @param uint64Count number of uint64 in the value.
     * @param bitIndex    the bit index.
     */
    public static void setBitUint(long[] value, int uint64Count, int bitIndex) {
        assert uint64Count > 0;
        assert bitIndex >= 0;
        assert bitIndex < Constants.UINT64_BITS * uint64Count;

        int uint64Index = bitIndex / Constants.UINT64_BITS;
        int subBitIndex = bitIndex % Constants.UINT64_BITS;
        value[uint64Index] |= (1L << subBitIndex);
    }

    /**
     * Gets if the specific bitIndex of the value is 1, where bitIndex = 0 means the left-most bit in the value.
     *
     * @param value       the value.
     * @param uint64Count number of uint64 in the value.
     * @param bitIndex    the bit index.
     * @return true if the specific bitIndex of the value is 1.
     */
    public static boolean isBitSetUint(long[] value, int uint64Count, int bitIndex) {
        assert uint64Count > 0;
        assert bitIndex >= 0;
        assert bitIndex < Constants.UINT64_BITS * uint64Count;

        int uint64Index = bitIndex / Constants.UINT64_BITS;
        int subBitIndex = bitIndex % Constants.UINT64_BITS;
        return ((value[uint64Index] >>> subBitIndex) & 1) != 0;
    }

    /**
     * Duplicates the input with length newUint64Count if needed.
     *
     * @param input          the input.
     * @param uint64Count    number of uint64 in the input.
     * @param newUint64Count number of uint64 in the output.
     * @param force          if true, then deep-copy, otherwise shallow-copy.
     * @return a new uint value that equals input.
     */
    public static long[] duplicateUintIfNeeded(long[] input, int uint64Count, int newUint64Count, boolean force) {
        if (!force && uint64Count >= newUint64Count) {
            return input;
        }
        long[] newUint = new long[newUint64Count];
        setUint(input, uint64Count, newUint, newUint64Count);
        return newUint;
    }


}
