package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

/**
 * Silver Coder Creator utilities.
 *
 * @author Hanwen Feng
 * @date 2022/3/12
 */
public class SilverCodeCreatorUtils {
    /**
     * private constructor.
     */
    private SilverCodeCreatorUtils() {
        // empty
    }

    /**
     * min log(n)
     */
    public static final int MIN_LOG_N = 14;
    /**
     * max log(n)
     */
    public static final int MAX_LOG_N = 24;

    /**
     * Silver Code type
     */
    public enum SilverCodeType {
        /**
         * Silver5
         */
        SILVER_5,
        /**
         * Silver11
         */
        SILVER_11,
    }

    /**
     * g (gap) for Silver5
     */
    private static final int SILVER5_GAP_VALUE = 16;
    /**
     * g (gap) for Silver11
     */
    private static final int SILVER11_GAP_VALUE = 32;

    /**
     * Gets g (gap) for the Silver Code.
     *
     * @param silverCodeType Silver Code type.
     * @return g.
     */
    static int getGap(SilverCodeType silverCodeType) {
        switch (silverCodeType) {
            case SILVER_5:
                return SILVER5_GAP_VALUE;
            case SILVER_11:
                return SILVER11_GAP_VALUE;
            default:
                throw new IllegalArgumentException("Invalid " + SilverCodeType.class.getSimpleName() + ": " + silverCodeType.name());
        }
    }

    /**
     * hamming weight for the left-matrix of Silver5
     */
    private static final int SILVER5_WEIGHT = 5;
    /**
     * hamming weight for the left-matrix of Silver11
     */
    private static final int SILVER11_WEIGHT = 11;

    /**
     * Gets hamming weight for the left-matrix of the Silver Code.
     *
     * @param silverCodeType Silver Code type.
     * @return hamming weight for the left-matrix of the Silver Code.
     */
    static int getWeight(SilverCodeType silverCodeType) {
        switch (silverCodeType) {
            case SILVER_5:
                return SILVER5_WEIGHT;
            case SILVER_11:
                return SILVER11_WEIGHT;
            default:
                throw new IllegalArgumentException("Invalid " + SilverCodeType.class.getSimpleName() + ": " + silverCodeType.name());
        }
    }

    /**
     * Silver5 left seed, used to generate left sub-matrices L = [A, B // D, E] for Silver5.
     * <p></p>
     * For t = 5, the first column has non-zero locations rows {0, 0.372071, 0.576568, 0.608917, 0.854475} · m.
     */
    private static final double[] SILVER5_LEFT_SEED = {0, 0.372071, 0.576568, 0.608917, 0.854475};
    /**
     * Silver11 left matrix seed, used to generate left sub-matrices L = [A, B // D, E] for Silver11.
     * <p></p>
     * for t = 11, the first column is {0, 0.00278835, 0.0883852, 0.238023, 0.240532, 0.274624, 0.390639, 0.531551,
     * 0.637619, 0.945265, 0.965874} · m
     */
    private static final double[] SILVER11_LEFT_SEED = {
        0, 0.00278835, 0.0883852, 0.238023, 0.240532, 0.274624, 0.390639, 0.531551, 0.637619, 0.945265, 0.965874
    };

    /**
     * Gets left matrix seed.
     * <p></p>
     * The left m × m sub-matrix L = [A, B // D, E] consists of weight t columns where each is a cyclic shift by 1 of
     * the previous. For small m, if two non-zero locations collide we insert into the next non-zero position.
     *
     * @param silverCodeType Silver Code type.
     * @return left matrix seed.
     */
    static double[] getLeftSeed(SilverCodeType silverCodeType) {
        switch (silverCodeType) {
            case SILVER_5:
                return SILVER5_LEFT_SEED;
            case SILVER_11:
                return SILVER11_LEFT_SEED;
            default:
                throw new IllegalArgumentException("Invalid " + SilverCodeType.class.getSimpleName() + ": " + silverCodeType.name());
        }
    }

    /**
     * Improved Silver5 right seed, used to generate the right sub-matrices [C // F] for Silver5.
     */
    private static final int[][] SILVER5_RIGHT_SEED = {
        {0, 2, 11, 14, 16, 21, 47},
        {0, 3, 8, 9, 16, 21, 47},
        {0, 1, 2, 4, 8, 21, 47},
        {0, 1, 10, 15, 16, 21, 47},
        {0, 7, 8, 12, 14, 21, 47},
        {0, 1, 2, 9, 10, 21, 47},
        {0, 2, 3, 8, 16, 21, 47},
        {0, 5, 6, 13, 14, 21, 47},
        {0, 3, 4, 11, 15, 21, 47},
        {0, 4, 6, 8, 12, 21, 47},
        {0, 2, 3, 7, 15, 21, 47},
        {0, 3, 5, 6, 8, 21, 47},
        {0, 6, 9, 12, 13, 21, 47},
        {0, 8, 10, 11, 13, 21, 47},
        {0, 1, 10, 12, 13, 21, 47},
        {0, 1, 7, 8, 16, 21, 47},
    };

    /**
     * Improved Silver11 right seed, used to generate right sub-matrices [C // F] for Silver11.
     */
    private static final int[][] SILVER11_RIGHT_SEED = {
        {0, 1, 2, 4, 15, 16, 17, 19, 24, 25, 26, 37, 63},
        {0, 1, 9, 10, 12, 18, 23, 24, 25, 30, 32, 37, 63},
        {0, 5, 6, 13, 14, 15, 16, 18, 19, 24, 31, 37, 63},
        {0, 2, 11, 14, 15, 19, 20, 22, 24, 28, 31, 37, 63},
        {0, 2, 3, 5, 6, 8, 19, 21, 22, 23, 27, 37, 63},
        {0, 1, 6, 8, 10, 12, 13, 16, 17, 21, 26, 37, 63},
        {0, 2, 4, 7, 8, 10, 24, 25, 26, 28, 29, 37, 63},
        {0, 4, 8, 10, 12, 13, 15, 23, 25, 26, 29, 37, 63},
        {0, 2, 6, 7, 8, 12, 17, 24, 27, 29, 30, 37, 63},
        {0, 2, 4, 5, 11, 12, 17, 19, 21, 22, 25, 37, 63},
        {0, 2, 5, 8, 9, 11, 12, 18, 20, 28, 32, 37, 63},
        {0, 4, 6, 8, 12, 17, 18, 19, 20, 22, 27, 37, 63},
        {0, 9, 16, 17, 20, 22, 23, 24, 27, 29, 30, 37, 63},
        {0, 3, 5, 7, 16, 17, 18, 20, 23, 30, 32, 37, 63},
        {0, 1, 2, 5, 7, 8, 9, 11, 19, 20, 28, 37, 63},
        {0, 3, 9, 11, 13, 15, 24, 27, 28, 31, 32, 37, 63},
        {0, 5, 10, 13, 14, 16, 19, 21, 26, 29, 32, 37, 63},
        {0, 1, 4, 7, 10, 12, 15, 20, 21, 24, 26, 37, 63},
        {0, 6, 10, 13, 15, 16, 17, 18, 19, 20, 21, 37, 63},
        {0, 4, 6, 7, 8, 18, 20, 24, 25, 27, 30, 37, 63},
        {0, 1, 4, 12, 13, 19, 25, 29, 30, 31, 32, 37, 63},
        {0, 2, 9, 15, 16, 19, 22, 23, 25, 28, 29, 37, 63},
        {0, 5, 7, 10, 15, 19, 21, 24, 26, 29, 30, 37, 63},
        {0, 4, 6, 11, 13, 15, 21, 25, 29, 31, 32, 37, 63},
        {0, 8, 10, 11, 15, 16, 17, 25, 27, 30, 32, 37, 63},
        {0, 3, 4, 5, 7, 14, 15, 16, 20, 28, 32, 37, 63},
        {0, 2, 3, 9, 11, 14, 15, 18, 19, 20, 25, 37, 63},
        {0, 10, 12, 13, 17, 19, 23, 27, 28, 29, 32, 37, 63},
        {0, 7, 8, 10, 12, 13, 16, 26, 28, 29, 31, 37, 63},
        {0, 6, 7, 11, 12, 15, 23, 27, 28, 30, 31, 37, 63},
        {0, 5, 6, 8, 10, 11, 14, 22, 25, 29, 30, 37, 63},
        {0, 2, 11, 14, 16, 17, 23, 24, 27, 30, 32, 37, 63},
    };

    /**
     * Gets right matrix seed.
     * <p></p>
     * In its original form, each row specifies the non-zero locations at a relative offset of g positions left of the
     * main diagonal, where g is 16 (for Silver5) and 32 (for Silver11), respectively.
     * <p></p>
     * We improve the right seed, each row specifies the non-zero locations for each column of the right sub-matrices
     * [C // F]. That is, row 0 specifies the non-zero locations for column 0 of the right sub-matrices [C // F].
     * There are g = 32 rows. For other columns of the right sub-matrices [C // F], we need to repeat the seed.
     *
     * @param silverCodeType Silver Code type.
     * @return right seed.
     */
    static int[][] getRightSeed(SilverCodeType silverCodeType) {
        switch (silverCodeType) {
            case SILVER_5:
                return SILVER5_RIGHT_SEED;
            case SILVER_11:
                return SILVER11_RIGHT_SEED;
            default:
                throw new IllegalArgumentException("Invalid " + SilverCodeType.class.getSimpleName() + ": " + silverCodeType.name());
        }
    }
}
