package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;

import java.security.SecureRandom;

/**
 * F2 -> F3 weak PRF matrix factory.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public class F23WprfMatrixFactory {
    /**
     * matrix type
     */
    public enum F23WprfMatrixType {
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
     * @param z3Field           Z3-field.
     * @param seed              the seed.
     * @param f23WprfMatrixType matrix type.
     * @return a matrix.
     */
    public static F23WprfMatrix createRandom(Z3ByteField z3Field, byte[] seed, F23WprfMatrixType f23WprfMatrixType) {
        return switch (f23WprfMatrixType) {
            case NAIVE -> F23WprfNaiveMatrix.createRandom(z3Field, seed);
            case BYTE -> F23WprfByteMatrix.createRandom(z3Field, seed);
            case LONG -> F23WprfLongMatrix.createRandom(z3Field, seed);
        };
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param z3Field           Z3-field.
     * @param secureRandom      random state.
     * @param f23WprfMatrixType matrix type.
     * @return a matrix.
     */
    public static F23WprfMatrix createRandom(Z3ByteField z3Field, SecureRandom secureRandom, F23WprfMatrixType f23WprfMatrixType) {
        return switch (f23WprfMatrixType) {
            case NAIVE -> F23WprfNaiveMatrix.createRandom(z3Field, secureRandom);
            case BYTE -> F23WprfByteMatrix.createRandom(z3Field, secureRandom);
            case LONG -> F23WprfLongMatrix.createRandom(z3Field, secureRandom);
        };
    }

    /**
     * Creates a matrix.
     *
     * @param z3Field           Z3-field.
     * @param elements          elements.
     * @param f23WprfMatrixType matrix type
     * @return a matrix.
     */
    public static F23WprfMatrix create(Z3ByteField z3Field, byte[][] elements, F23WprfMatrixType f23WprfMatrixType) {
        return switch (f23WprfMatrixType) {
            case NAIVE -> F23WprfNaiveMatrix.create(z3Field, elements);
            case BYTE -> F23WprfByteMatrix.create(z3Field, elements);
            case LONG -> F23WprfLongMatrix.create(z3Field, elements);
        };
    }
}
