package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3Utils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32WprfMatrixFactory.F32WprfMatrixType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * F3 -> F2 weak PRF matrix with computation in byte array.
 *
 * @author Feng Han
 * @date 2024/10/16
 */
public class F32WprfByteMatrix implements F32WprfMatrix {
    /**
     * byte column number
     */
    static final int COLUMN_BYTES = F32Wprf.M / 4;
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;
    /**
     * elements
     */
    private byte[][] elements;
    /**
     * double elements
     */
    private byte[][] doubleElements;

    /**
     * Creates a matrix.
     *
     * @param z3Field  Z3-field.
     * @param elements elements.
     * @return a matrix.
     */
    static F32WprfByteMatrix create(Z3ByteField z3Field, byte[][] elements) {
        F32WprfByteMatrix matrix = createZeros(z3Field);
        for (int iRow = 0; iRow < ROWS; iRow++) {
            byte[] originalMul2 = new byte[COLUMNS];
            for (int iCol = 0; iCol < COLUMNS; iCol++) {
                originalMul2[iCol] = z3Field.mul(elements[iRow][iCol], (byte) 2);
            }
            matrix.elements[iRow] = Z3Utils.compressToByteArray(elements[iRow]);
            matrix.doubleElements[iRow] = Z3Utils.compressToByteArray(originalMul2);
        }
        return matrix;
    }

    /**
     * Creates an all-zero matrix.
     *
     * @param z3Field Z3-field.
     * @return a matrix.
     */
    private static F32WprfByteMatrix createZeros(Z3ByteField z3Field) {
        F32WprfByteMatrix matrix = new F32WprfByteMatrix(z3Field);
        matrix.elements = new byte[ROWS][COLUMN_BYTES];
        matrix.doubleElements = new byte[ROWS][COLUMN_BYTES];
        return matrix;
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param z3Field      Z3-field.
     * @param secureRandom random state.
     * @return a matrix.
     */
    static F32WprfByteMatrix createRandom(Z3ByteField z3Field, SecureRandom secureRandom) {
        F32WprfByteMatrix matrix = createZeros(z3Field);
        int prime = matrix.z3Field.getPrime();
        for (int iRow = 0; iRow < ROWS; iRow++) {
            byte[] original = new byte[COLUMNS];
            byte[] originalMul2 = new byte[COLUMNS];
            for (int iCol = 0; iCol < COLUMNS; iCol++) {
                original[iCol] = (byte) secureRandom.nextInt(prime);
                originalMul2[iCol] = z3Field.mul(original[iCol], (byte) 2);
            }
            matrix.elements[iRow] = Z3Utils.compressToByteArray(original);
            matrix.doubleElements[iRow] = Z3Utils.compressToByteArray(originalMul2);
        }
        return matrix;
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param z3Field Z3-field.
     * @param seed    the seed.
     * @return a matrix.
     */
    static F32WprfByteMatrix createRandom(Z3ByteField z3Field, byte[] seed) {
        assert seed.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // we use SHA1PRNG to generate the matrix
        SecureRandom secureRandom = CommonUtils.createSeedSecureRandom();
        secureRandom.setSeed(seed);
        return createRandom(z3Field, secureRandom);
    }

    private F32WprfByteMatrix(Z3ByteField z3Field) {
        this.z3Field = z3Field;
    }

    /**
     * Left Multiplication.
     *
     * @param vector the vector.
     * @return the result.
     */
    public byte[] leftMul(byte[] vector) {
        assert vector.length == ROWS;
        // verify elements
        for (byte b : vector) {
            assert z3Field.validateElement(b);
        }
        // we use int array to combine without mod
        byte[] byteOut = new byte[COLUMN_BYTES];
        for (int i = 0; i < ROWS; i++) {
            if (vector[i] == 1) {
                Z3Utils.uncheckCompressByteAddi(byteOut, elements[i]);
            } else if (vector[i] == 2) {
                Z3Utils.uncheckCompressByteAddi(byteOut, doubleElements[i]);
            }
        }
        return Z3Utils.decompressFromByteArray(byteOut, COLUMNS);
    }

    @Override
    public F32WprfMatrixType getType() {
        return F32WprfMatrixType.BYTE;
    }

    @Override
    public F32WprfMatrix copy() {
        F32WprfByteMatrix copy = new F32WprfByteMatrix(z3Field);
        copy.elements = BytesUtils.clone(elements);
        return copy;
    }

    @Override
    public int getRows() {
        return ROWS;
    }

    @Override
    public int getColumns() {
        return COLUMNS;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(elements).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof F32WprfByteMatrix that)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return new EqualsBuilder().append(this.elements, that.elements).isEquals();
    }

}
