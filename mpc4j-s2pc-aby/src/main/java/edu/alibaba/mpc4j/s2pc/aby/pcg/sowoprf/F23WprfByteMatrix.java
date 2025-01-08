package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3Utils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23WprfMatrixFactory.F23WprfMatrixType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * F2 -> F3 weak PRF byte matrix.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public class F23WprfByteMatrix implements F23WprfMatrix {
    /**
     * number of columns represented in byte, we can use 21 bytes, (84 elements in Z_3) to represent number of columns.
     */
    private static final int COLUMN_BYTES = CommonUtils.getUnitNum(COLUMNS, Byte.SIZE / 2);
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
    static F23WprfByteMatrix create(Z3ByteField z3Field, byte[][] elements) {
        assert elements.length == ROWS;
        F23WprfByteMatrix matrix = createZeros(z3Field);
        for (int iRow = 0; iRow < ROWS; iRow++) {
            assert elements[iRow].length == COLUMNS;
            byte[] doubleRow = new byte[COLUMNS];
            for (int iCol = 0; iCol < COLUMNS; iCol++) {
                doubleRow[iCol] = z3Field.mul(elements[iRow][iCol], (byte) 2);
            }
            matrix.elements[iRow] = Z3Utils.compressToByteArray(elements[iRow]);
            matrix.doubleElements[iRow] = Z3Utils.compressToByteArray(doubleRow);
        }
        return matrix;
    }

    /**
     * Creates an all-zero matrix.
     *
     * @param z3Field Z3-field.
     * @return a matrix.
     */
    static F23WprfByteMatrix createZeros(Z3ByteField z3Field) {
        F23WprfByteMatrix matrix = new F23WprfByteMatrix(z3Field);
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
    static F23WprfByteMatrix createRandom(Z3ByteField z3Field, SecureRandom secureRandom) {
        F23WprfByteMatrix matrix = createZeros(z3Field);
        int prime = matrix.z3Field.getPrime();
        for (int iRow = 0; iRow < ROWS; iRow++) {
            byte[] row = new byte[COLUMNS];
            byte[] doubleRow = new byte[COLUMNS];
            for (int iCol = 0; iCol < COLUMNS; iCol++) {
                row[iCol] = (byte) secureRandom.nextInt(prime);
                doubleRow[iCol] = z3Field.mul(row[iCol], (byte) 2);
            }
            matrix.elements[iRow] = Z3Utils.compressToByteArray(row);
            matrix.doubleElements[iRow] = Z3Utils.compressToByteArray(doubleRow);
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
    static F23WprfByteMatrix createRandom(Z3ByteField z3Field, byte[] seed) {
        assert seed.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // we use SHA1PRNG to generate the matrix
        SecureRandom secureRandom = CommonUtils.createSeedSecureRandom();
        secureRandom.setSeed(seed);
        return createRandom(z3Field, secureRandom);
    }

    private F23WprfByteMatrix(Z3ByteField z3Field) {
        this.z3Field = z3Field;
    }

    /**
     * Left Multiplication.
     *
     * @param vector the vector.
     * @return the result.
     */
    public byte[] leftBinaryMul(byte[] vector) {
        assert vector.length == ROW_BINARY_BYTES;
        byte[] result = new byte[COLUMN_BYTES];
        for (int i = 0; i < ROWS; i++) {
            if (BinaryUtils.getBoolean(vector, i)) {
                Z3Utils.uncheckCompressByteAddi(result, elements[i]);
            }
        }
        return Z3Utils.decompressFromByteArray(result, COLUMNS);
    }

    @Override
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
    public byte[] leftCompressMul(byte[] vector) {
        assert vector.length == ROW_BYTES;
        byte[] byteOut = new byte[COLUMN_BYTES];
        for (int i = 0; i < ROWS; i++) {
            if (BinaryUtils.getBoolean(vector, i * 2)) {
                // vector[i] == 2
                Z3Utils.uncheckCompressByteAddi(byteOut, doubleElements[i]);
            } else if (BinaryUtils.getBoolean(vector, i * 2 + 1)) {
                // vector[i] == 1
                Z3Utils.uncheckCompressByteAddi(byteOut, elements[i]);
            }
        }
        return Z3Utils.decompressFromByteArray(byteOut, COLUMNS);
    }

    @Override
    public byte[] leftCompressMul(long[] vector) {
        assert vector.length == ROW_LONGS;
        byte[] byteOut = new byte[COLUMN_BYTES];
        for (int i = 0; i < ROWS; i++) {
            if (BinaryUtils.getBoolean(vector, i * 2)) {
                // vector[i] == 2
                Z3Utils.uncheckCompressByteAddi(byteOut, doubleElements[i]);
            } else if (BinaryUtils.getBoolean(vector, i * 2 + 1)) {
                // vector[i] == 1
                Z3Utils.uncheckCompressByteAddi(byteOut, elements[i]);
            }
        }
        return Z3Utils.decompressFromByteArray(byteOut, COLUMNS);
    }

    @Override
    public F23WprfMatrixType getType() {
        return F23WprfMatrixType.BYTE;
    }

    @Override
    public F23WprfByteMatrix copy() {
        F23WprfByteMatrix copy = new F23WprfByteMatrix(z3Field);
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
        if (!(obj instanceof F23WprfByteMatrix that)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return new EqualsBuilder().append(this.elements, that.elements).isEquals();
    }
}
