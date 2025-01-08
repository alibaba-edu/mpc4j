package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;

import java.security.SecureRandom;

/**
 * F3 -> F2 weak PRF matrix factory.
 *
 * @author Feng Han
 * @date 2024/10/16
 */
public class F32WprfMatrixFactory {
    /**
     * matrix type
     */
    public enum F32WprfMatrixType {
        /**
         * NAIVE, each element in Z_3 is stored in one byte.
         */
        NAIVE,
        /**
         * BYTE, each 4 elements in Z_3 is stored in one byte.
         */
        BYTE,
        /**
         * LONG, each 32 elements in Z_3 is stored in one long.
         */
        LONG,
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param z3Field    Z3-field.
     * @param seed       the seed.
     * @param f32WprfMatrixType matrix type
     * @return a matrix.
     */
    public static F32WprfMatrix createRandom(Z3ByteField z3Field, byte[] seed, F32WprfMatrixType f32WprfMatrixType) {
        return switch (f32WprfMatrixType) {
            case NAIVE -> F32WprfNaiveMatrix.createRandom(z3Field, seed);
            case BYTE -> F32WprfByteMatrix.createRandom(z3Field, seed);
            case LONG -> F32WprfLongMatrix.createRandom(z3Field, seed);
        };
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param z3Field      Z3-field.
     * @param secureRandom random state.
     * @param f32WprfMatrixType   matrix type
     * @return a matrix.
     */
    public static F32WprfMatrix createRandom(Z3ByteField z3Field, SecureRandom secureRandom, F32WprfMatrixType f32WprfMatrixType) {
        return switch (f32WprfMatrixType) {
            case NAIVE -> F32WprfNaiveMatrix.createRandom(z3Field, secureRandom);
            case BYTE -> F32WprfByteMatrix.createRandom(z3Field, secureRandom);
            case LONG -> F32WprfLongMatrix.createRandom(z3Field, secureRandom);
        };
    }

    /**
     * Creates a matrix.
     *
     * @param z3Field    Z3-field.
     * @param elements   elements.
     * @param f32WprfMatrixType matrix type
     * @return a matrix.
     */
    public static F32WprfMatrix create(Z3ByteField z3Field, byte[][] elements, F32WprfMatrixType f32WprfMatrixType) {
        return switch (f32WprfMatrixType) {
            case NAIVE -> F32WprfNaiveMatrix.create(z3Field, elements);
            case BYTE -> F32WprfByteMatrix.create(z3Field, elements);
            case LONG -> F32WprfLongMatrix.create(z3Field, elements);
        };
    }
}
