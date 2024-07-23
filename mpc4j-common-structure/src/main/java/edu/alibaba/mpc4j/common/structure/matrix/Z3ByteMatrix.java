package edu.alibaba.mpc4j.common.structure.matrix;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * Z3 byte matrix.
 *
 * @author Weiran Liu
 * @date 2024/5/22
 */
public class Z3ByteMatrix implements Matrix {
    /**
     * Z3-field
     */
    public final Z3ByteField z3Field;
    /**
     * elements
     */
    public byte[][] elements;
    /**
     * rows
     */
    public int rows;
    /**
     * cols
     */
    public int cols;

    /**
     * Creates a matrix.
     *
     * @param z3Field Z3-field.
     * @param elements elements.
     * @return a matrix.
     */
    public static Z3ByteMatrix create(Z3ByteField z3Field, byte[][] elements) {
        int rows = elements.length;
        MathPreconditions.checkPositive("rows", rows);
        int cols = elements[0].length;
        MathPreconditions.checkPositive("cols", cols);
        Z3ByteMatrix matrix = new Z3ByteMatrix(z3Field);
        for (int iRow = 0; iRow < rows; iRow++) {
            MathPreconditions.checkEqual(iRow + "-th row.length", "cols", elements[iRow].length, cols);
            for (int iCol = 0; iCol < cols; iCol++) {
                Preconditions.checkArgument(matrix.z3Field.validateElement(elements[iRow][iCol]));
            }
        }
        matrix.elements = elements;
        matrix.rows = rows;
        matrix.cols = cols;
        return matrix;
    }

    /**
     * Creates an all-zero matrix.
     *
     * @param z3Field Z3-field.
     * @param rows rows.
     * @param cols cols.
     * @return a matrix.
     */
    public static Z3ByteMatrix createZeros(Z3ByteField z3Field, int rows, int cols) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("cols", cols);
        Z3ByteMatrix matrix = new Z3ByteMatrix(z3Field);
        matrix.elements = new byte[rows][cols];
        matrix.rows = rows;
        matrix.cols = cols;
        return matrix;
    }

    /**
     * Creates a random matrix.
     *
     * @param z3Field Z3-field.
     * @param rows         rows.
     * @param cols         cols.
     * @param secureRandom the random state.
     * @return a matrix.
     */
    public static Z3ByteMatrix createRandom(Z3ByteField z3Field, int rows, int cols, SecureRandom secureRandom) {
        Z3ByteMatrix matrix = createZeros(z3Field, rows, cols);
        for (int iRow = 0; iRow < rows; iRow++) {
            for (int iCol = 0; iCol < cols; iCol++) {
                matrix.elements[iRow][iCol] = matrix.z3Field.createRandom(secureRandom);
            }
        }
        return matrix;
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param z3Field Z3-field.
     * @param rows rows.
     * @param cols cols.
     * @param seed the seed.
     * @return a matrix.
     */
    public static Z3ByteMatrix createRandom(Z3ByteField z3Field, int rows, int cols, byte[] seed) {
        MathPreconditions.checkEqual("λ", "seed.length", CommonConstants.BLOCK_BYTE_LENGTH, seed.length);
        Z3ByteMatrix matrix = createZeros(z3Field, rows, cols);
        // we use SHA1PRNG to generate the matrix
        SecureRandom secureRandom = CommonUtils.createSeedSecureRandom();
        secureRandom.setSeed(seed);
        int prime = matrix.z3Field.getPrime();
        for (int iRow = 0; iRow < rows; iRow++) {
            for (int iCol = 0; iCol < cols; iCol++) {
                matrix.elements[iRow][iCol] = (byte) secureRandom.nextInt(prime);
            }
        }
        return matrix;
    }

    private Z3ByteMatrix(Z3ByteField z3Field) {
        this.z3Field = z3Field;
    }

    /**
     * Left Multiplication.
     *
     * @param vector the vector.
     * @return the result.
     */
    public byte[] leftMul(byte[] vector) {
        MathPreconditions.checkEqual("rows", "vector.length", rows, vector.length);
        // verify elements
        for (byte b : vector) {
            Preconditions.checkArgument(z3Field.validateElement(b));
        }
        // we use int array to combine without mod
        int[] intOutput = new int[cols];
        for (int i = 0; i < rows; i++) {
            if (vector[i] == 1) {
                for (int j = 0; j < cols; j++) {
                    intOutput[j] += elements[i][j];
                }
            } else if (vector[i] == 2) {
                for (int j = 0; j < cols; j++) {
                    intOutput[j] += (elements[i][j] << 1);
                }
            }
        }
        // mod output
        byte[] output = new byte[cols];
        for (int j = 0; j < cols; j++) {
            output[j] = (byte) (intOutput[j] % 3);
            if (output[j] < 0) {
                output[j] += z3Field.getPrime();
            }
            assert z3Field.validateElement(output[j]);
        }
        return output;
    }

    @Override
    public Z3ByteMatrix copy() {
        Z3ByteMatrix copy = new Z3ByteMatrix(z3Field);
        copy.rows = rows;
        copy.cols = cols;
        copy.elements = BytesUtils.clone(elements);
        return copy;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public int getColumns() {
        return cols;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(elements).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Z3ByteMatrix that)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return new EqualsBuilder().append(this.elements, that.elements).isEquals();
    }
}
