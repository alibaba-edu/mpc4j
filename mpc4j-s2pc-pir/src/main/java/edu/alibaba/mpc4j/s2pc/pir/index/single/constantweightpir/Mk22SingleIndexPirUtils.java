package edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * provide constant-weight code and folklore code implementation
 *
 * @author Qixian Zhou
 * @date 2023/6/19
 */
public class Mk22SingleIndexPirUtils {

    /**
     * Perfect constant weight codeword mapping.
     *
     * @param input             input.
     * @param codewordBitLength m.
     * @param hammingWeight     k.
     * @return constant weight codeword.
     */
    public static int[] getPerfectConstantWeightCodeword(int input, int codewordBitLength, int hammingWeight) {
        int n = (int) DoubleUtils.estimateCombinatorial(codewordBitLength, hammingWeight);
        assert input < n : "input must be smaller than size of the codewords";
        int[] codeword = new int[codewordBitLength];
        int remainder = input;
        int kPrime = hammingWeight;
        for (int p = codewordBitLength - 1; p >= 0; p--) {
            int temp;
            if (kPrime > p) {
                temp = 0;
            } else {
                temp = (int) DoubleUtils.estimateCombinatorial(p, kPrime);
            }
            if (remainder >= temp) {
                codeword[p] = 1;
                remainder -= temp;
                kPrime -= 1;
            }
        }
        return codeword;
    }

    /**
     * get folklore codeword.
     *
     * @param input             input.
     * @param codewordBitLength codeword length.
     * @return codeword.
     */
    public static int[] getFolkloreCodeword(int input, int codewordBitLength) {
        int n = 1 << codewordBitLength;
        assert input < n : "input must be smaller than size of the codewords";
        int[] codeword = new int[codewordBitLength];
        int remainder = input;
        for (int p = 0; p < codewordBitLength; p++) {
            codeword[p] = remainder % 2;
            remainder /= 2;
        }
        return codeword;
    }
}