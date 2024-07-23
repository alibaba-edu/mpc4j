package edu.alibaba.mpc4j.common.tool.galoisfield;

/**
 * Galois field test utilities.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class GaloisfieldTestUtils {
    /**
     * private constructor
     */
    private GaloisfieldTestUtils() {
        // empty
    }

    /**
     * GF(2^l) array
     */
    public static int[] GF2E_L_ARRAY = new int[]{
        1, 2, 3, 4, 7, 8, 9, 15, 16, 17, 31, 32, 33, 39, 40, 41, 63, 64, 65, 127, 128
    };
}
