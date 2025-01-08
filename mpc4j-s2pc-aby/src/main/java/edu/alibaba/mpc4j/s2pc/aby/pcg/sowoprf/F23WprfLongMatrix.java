package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3Utils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23WprfMatrixFactory.F23WprfMatrixType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * F2 -> F3 weak PRF matrix B ∈ F_3^{t×m}, where m = 2λ, t = λ / log2(3).
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public class F23WprfLongMatrix implements F23WprfMatrix {
    /**
     * number of columns represented in long, we can use 3 longs (24 bytes, 96 Z_3) to represent number of columns.
     */
    private static final int COLUMN_LONGS = CommonUtils.getUnitNum(COLUMNS, Long.SIZE / 2);
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;
    /**
     * elements
     */
    private long[][] elements;
    /**
     * double elements
     */
    private long[][] doubleElements;

    /**
     * Creates a matrix.
     *
     * @param z3Field  Z3-field.
     * @param elements elements.
     * @return a matrix.
     */
    static F23WprfLongMatrix create(Z3ByteField z3Field, byte[][] elements) {
        assert elements.length == ROWS;
        F23WprfLongMatrix matrix = createZeros(z3Field);
        for (int iRow = 0; iRow < ROWS; iRow++) {
            assert elements[iRow].length == COLUMNS;
            byte[] doubleRow = new byte[COLUMNS];
            for (int iCol = 0; iCol < COLUMNS; iCol++) {
                doubleRow[iCol] = z3Field.mul(elements[iRow][iCol], (byte) 2);
            }
            matrix.elements[iRow] = Z3Utils.compressToLongArray(elements[iRow]);
            matrix.doubleElements[iRow] = Z3Utils.compressToLongArray(doubleRow);
        }
        return matrix;
    }

    /**
     * Creates an all-zero matrix.
     *
     * @param z3Field Z3-field.
     * @return a matrix.
     */
    static F23WprfLongMatrix createZeros(Z3ByteField z3Field) {
        F23WprfLongMatrix matrix = new F23WprfLongMatrix(z3Field);
        matrix.elements = new long[ROWS][COLUMN_LONGS];
        matrix.doubleElements = new long[ROWS][COLUMN_LONGS];
        return matrix;
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param z3Field      Z3-field.
     * @param secureRandom random state.
     * @return a matrix.
     */
    static F23WprfLongMatrix createRandom(Z3ByteField z3Field, SecureRandom secureRandom) {
        F23WprfLongMatrix matrix = createZeros(z3Field);
        int prime = matrix.z3Field.getPrime();
        for (int iRow = 0; iRow < ROWS; iRow++) {
            byte[] row = new byte[COLUMNS];
            byte[] doubleRow = new byte[COLUMNS];
            for (int iCol = 0; iCol < COLUMNS; iCol++) {
                row[iCol] = (byte) secureRandom.nextInt(prime);
                doubleRow[iCol] = z3Field.mul(row[iCol], (byte) 2);
            }
            matrix.elements[iRow] = Z3Utils.compressToLongArray(row);
            matrix.doubleElements[iRow] = Z3Utils.compressToLongArray(doubleRow);
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
    static F23WprfLongMatrix createRandom(Z3ByteField z3Field, byte[] seed) {
        assert seed.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // we use SHA1PRNG to generate the matrix
        SecureRandom secureRandom = CommonUtils.createSeedSecureRandom();
        secureRandom.setSeed(seed);
        return createRandom(z3Field, secureRandom);
    }

    private F23WprfLongMatrix(Z3ByteField z3Field) {
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
        long[] result = new long[COLUMN_LONGS];
        for (int i = 0; i < ROWS; i++) {
            if (BinaryUtils.getBoolean(vector, i)) {
                Z3Utils.uncheckCompressLongAddi(result, elements[i]);
            }
        }
        return Z3Utils.decompressFromLongArray(result, COLUMNS);
    }

    @Override
    public byte[] leftMul(byte[] vector) {
        assert vector.length == ROWS;
        // verify elements
        for (byte b : vector) {
            assert z3Field.validateElement(b);
        }
        // we use int array to combine without mod
        long[] byteOut = new long[COLUMN_LONGS];
        for (int i = 0; i < ROWS; i++) {
            if (vector[i] == 1) {
                Z3Utils.uncheckCompressLongAddi(byteOut, elements[i]);
            } else if (vector[i] == 2) {
                Z3Utils.uncheckCompressLongAddi(byteOut, doubleElements[i]);
            }
        }
        return Z3Utils.decompressFromLongArray(byteOut, COLUMNS);
    }

    @Override
    public byte[] leftCompressMul(byte[] vector) {
        assert vector.length == ROW_BYTES;
        long[] byteOut = new long[COLUMN_LONGS];
        for (int i = 0; i < ROWS; i++) {
            if (BinaryUtils.getBoolean(vector, i * 2)) {
                // vector[i] == 2
                Z3Utils.uncheckCompressLongAddi(byteOut, doubleElements[i]);
            } else if (BinaryUtils.getBoolean(vector, i * 2 + 1)) {
                // vector[i] == 1
                Z3Utils.uncheckCompressLongAddi(byteOut, elements[i]);
            }
        }
        return Z3Utils.decompressFromLongArray(byteOut, COLUMNS);
    }

    @Override
    public byte[] leftCompressMul(long[] vector) {
        assert vector.length == ROW_LONGS;
        long[] byteOut = new long[COLUMN_LONGS];
        for (int i = 0; i < ROWS; i++) {
            if (BinaryUtils.getBoolean(vector, i * 2)) {
                // vector[i] == 2
                Z3Utils.uncheckCompressLongAddi(byteOut, doubleElements[i]);
            } else if (BinaryUtils.getBoolean(vector, i * 2 + 1)) {
                // vector[i] == 1
                Z3Utils.uncheckCompressLongAddi(byteOut, elements[i]);
            }
        }
        return Z3Utils.decompressFromLongArray(byteOut, COLUMNS);
    }

    @Override
    public F23WprfMatrixType getType() {
        return F23WprfMatrixType.LONG;
    }

    @Override
    public F23WprfLongMatrix copy() {
        F23WprfLongMatrix copy = new F23WprfLongMatrix(z3Field);
        copy.elements = LongUtils.clone(elements);
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
        if (!(obj instanceof F23WprfLongMatrix that)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return new EqualsBuilder().append(this.elements, that.elements).isEquals();
    }
}
