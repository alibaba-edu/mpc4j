package edu.alibaba.mpc4j.common.tool.okve.basic;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Basic OKVS factory.
 *
 * @author Weiran Liu
 * @date 2023/3/27
 */
public class BasicOkvsFactory {
    /**
     * private constructor.
     */
    private BasicOkvsFactory() {
        // empty
    }

    public enum BasicOkvsType {
        /**
         * polynomial interpolation
         */
        POLYNOMIAL,
        /**
         * MegaBin
         */
        MEGA_BIN,
    }

    /**
     * Creates a field OKVS.
     *
     * @param envType the environment.
     * @param type    the type.
     * @param n       the number of key-value pairs.
     * @param l       the input / output bit length, must satifies l % Byte.SIZE == 0.
     * @param keys    the hash keys.
     * @return a field OKVS.
     */
    public static BasicOkvs createInstance(EnvType envType, BasicOkvsType type, int n, int l, byte[][] keys) {
        assert keys.length == getHashNum(type);
        switch (type) {
            case POLYNOMIAL:
                return new PolyBasicOkvs(envType, n, l);
            case MEGA_BIN:
                return new MegaBinBasicOkvs(envType, n, l, keys[0]);
            default:
                throw new IllegalArgumentException("Invalid " + BasicOkvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets the number of hashes.
     *
     * @param type the type.
     * @return the number of hashes.
     */
    public static int getHashNum(BasicOkvsType type) {
        switch (type) {
            case POLYNOMIAL:
                return 0;
            case MEGA_BIN:
                return 1;
            default:
                throw new IllegalArgumentException("Invalid " + BasicOkvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets m.
     *
     * @param type the type.
     * @param n    the number of key-value pairs.
     * @return m.
     */
    public static int getM(BasicOkvsType type, int n) {
        assert n > 0 : "n must be greater than 0: " + n;
        switch (type) {
            case POLYNOMIAL:
                return n;
            case MEGA_BIN:
                int binNum = MegaBinBasicOkvs.getBinNum(n);
                int binSize = MegaBinBasicOkvs.getBinSize(n);
                return binNum * binSize;
            default:
                throw new IllegalArgumentException("Invalid " + BasicOkvsType.class.getSimpleName() + ": " + type.name());
        }
    }
}
