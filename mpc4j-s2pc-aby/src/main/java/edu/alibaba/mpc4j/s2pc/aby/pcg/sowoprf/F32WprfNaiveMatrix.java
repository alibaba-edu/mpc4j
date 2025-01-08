package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32WprfMatrixFactory.F32WprfMatrixType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * F3 -> F2 weak PRF matrix with naive implementation.
 *
 * @author Weiran Liu
 * @date 2024/10/16
 */
public class F32WprfNaiveMatrix implements F32WprfMatrix {
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
     * @param z3Field Z3-field.
     * @param elements elements.
     * @return a matrix.
     */
    static F32WprfNaiveMatrix create(Z3ByteField z3Field, byte[][] elements) {
        F32WprfNaiveMatrix matrix = createZeros(z3Field);
        for (int iRow = 0; iRow < ROWS; iRow++) {
            for (int iCol = 0; iCol < COLUMNS; iCol++) {
                matrix.elements[iRow][iCol] = elements[iRow][iCol];
                matrix.doubleElements[iRow][iCol] = (byte) (matrix.elements[iRow][iCol] << 1);
            }
        }
        return matrix;
    }

    /**
     * Creates an all-zero matrix.
     *
     * @param z3Field Z3-field.
     * @return a matrix.
     */
    static F32WprfNaiveMatrix createZeros(Z3ByteField z3Field) {
        F32WprfNaiveMatrix matrix = new F32WprfNaiveMatrix(z3Field);
        matrix.elements = new byte[ROWS][COLUMNS];
        matrix.doubleElements = new byte[ROWS][COLUMNS];
        return matrix;
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param z3Field Z3-field.
     * @param secureRandom random state.
     * @return a matrix.
     */
    static F32WprfNaiveMatrix createRandom(Z3ByteField z3Field, SecureRandom secureRandom) {
        F32WprfNaiveMatrix matrix = createZeros(z3Field);
        int prime = matrix.z3Field.getPrime();
        for (int iRow = 0; iRow < ROWS; iRow++) {
            for (int iCol = 0; iCol < COLUMNS; iCol++) {
                matrix.elements[iRow][iCol] = (byte) secureRandom.nextInt(prime);
                matrix.doubleElements[iRow][iCol] = (byte) (matrix.elements[iRow][iCol] << 1);
            }
        }
        return matrix;
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param z3Field Z3-field.
     * @param seed the seed.
     * @return a matrix.
     */
    static F32WprfNaiveMatrix createRandom(Z3ByteField z3Field, byte[] seed) {
        assert seed.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // we use SHA1PRNG to generate the matrix
        SecureRandom secureRandom = CommonUtils.createSeedSecureRandom();
        secureRandom.setSeed(seed);
        return createRandom(z3Field, secureRandom);
    }

    private F32WprfNaiveMatrix(Z3ByteField z3Field) {
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
        int[] intOutput = new int[COLUMNS];
        for (int i = 0; i < ROWS; i++) {
            if (vector[i] == 1) {
                for (int j = 0; j < COLUMNS; j++) {
                    intOutput[j] += elements[i][j];
                }
            } else if (vector[i] == 2) {
                for (int j = 0; j < COLUMNS; j++) {
                    intOutput[j] += doubleElements[i][j];
                }
            }
        }
        // mod output
        byte[] output = new byte[COLUMNS];
        for (int j = 0; j < COLUMNS; j++) {
            output[j] = (byte) (intOutput[j] % 3);
            if (output[j] < 0) {
                output[j] += z3Field.getPrime();
            }
            assert z3Field.validateElement(output[j]);
        }
        return output;
    }

    @Override
    public F32WprfMatrixType getType() {
        return F32WprfMatrixType.NAIVE;
    }

    @Override
    public F32WprfNaiveMatrix copy() {
        F32WprfNaiveMatrix copy = new F32WprfNaiveMatrix(z3Field);
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
        if (!(obj instanceof F32WprfNaiveMatrix that)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return new EqualsBuilder().append(this.elements, that.elements).isEquals();
    }
}
