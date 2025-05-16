package edu.alibaba.mpc4j.common.tool.bristol;

/**
 * Gray code generator.
 * <p>
 * Gray code is an ordering of the binary numeral system such that two successive values differ in only one bit
 * (binary digit). For example, The binary code of {0 ... 7} is
 * <ul>
 *     <li>{000, 001, 010, 011, 100, 101, 110, 111}</li>
 * </ul>
 * while the Gray code of {0 ... 7} is
 * <ul>
 *     <li>{000, 001, 011, 010, 110, 111, 101, 100}</li>
 * </ul>
 *
 * @author Weiran Liu
 * @date 2025/4/7
 */
public class GrayCodeGenerator {
    /**
     * private constructor.
     */
    public GrayCodeGenerator() {
        // empty
    }

    /**
     * Computes the different positions of the Gray Code of n and the Gray Code of n - 1.
     *
     * @param n number.
     * @return the different positions of the Gray Code of n and the Gray Code of n - 1.
     */
    static int ctz(int n) {
        assert n >= 1;
        return Integer.bitCount(n ^ (n - 1)) - 1;
    }

    /**
     * Generates the Gray code of [1 ... n].
     *
     * @param n number.
     * @return Gray code of [1 ... n].
     */
    static int[] generate(int n) {
        assert n >= 1;
        int[] code = new int[n];
        code[0] = 0;
        for (int i = 1; i < n; i++) {
            code[i] = code[i - 1] ^ (1 << ctz(i));
        }
        return code;
    }
}
