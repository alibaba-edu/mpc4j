package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.SimdTransBitMatrixFactory.SimdTransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * unit tests for SIMD transpose bit matrix.
 *
 * @author Weiran Liu
 * @date 2024/3/9
 */
@RunWith(Parameterized.class)
public class SimdTransBitMatrixTest {
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * types
     */
    private static final SimdTransBitMatrixType[] TYPES = new SimdTransBitMatrixType[] {
        SimdTransBitMatrixType.VECTOR_64,
        SimdTransBitMatrixType.VECTOR_128,
        SimdTransBitMatrixType.JDK,
        SimdTransBitMatrixType.JDK_SPLIT_ROW,
        SimdTransBitMatrixType.JDK_SPLIT_COL,
    };

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (SimdTransBitMatrixType type : TYPES) {
            configurations.add(new Object[] {type.name(), type, });
        }

        return configurations;
    }

    /**
     * type
     */
    private final SimdTransBitMatrixType type;

    public SimdTransBitMatrixTest(String name, SimdTransBitMatrixType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        SimdTransBitMatrix a = SimdTransBitMatrixFactory.createInstance(
            type, CommonConstants.BLOCK_BIT_LENGTH, CommonConstants.BLOCK_BIT_LENGTH
        );
        Assert.assertEquals(type, a.getType());
        // same type after transpose
        SimdTransBitMatrix b = a.transpose();
        Assert.assertEquals(type, b.getType());
    }

    /**
     * (1 × 1)
     *
     * 1    -->     1
     */
    private static final int ROWS_MATRIX_1X1 = 1;
    private static final byte[][] ROW_MATRIX_1X1 = new byte[][] {
        new byte[] {0b00000001, },
    };
    private static final int COLUMNS_MATRIX_1X1 = 1;
    private static final byte[][] COLUMN_MATRIX_1X1 = new byte[][] {
        new byte[] {0b00000001, },
    };

    @Test
    public void test1X1() {
        testBitMatrix(ROWS_MATRIX_1X1, COLUMNS_MATRIX_1X1, ROW_MATRIX_1X1, COLUMN_MATRIX_1X1);
    }

    /**
     * (8 × 8)
     *
     * 00000001         00000000
     * 00000011         00000011
     * 00000111         00000111
     * 00001111   -->   00001111
     * 00011111         00011111
     * 00111111         00111111
     * 01111111         01111111
     * 01111111         11111111
     */
    private static final int ROWS_MATRIX_8X8 = 8;
    private static final byte[][] ROW_MATRIX_8X8 = new byte[][] {
        new byte[] {(byte)0b00000001, },
        new byte[] {(byte)0b00000011, },
        new byte[] {(byte)0b00000111, },
        new byte[] {(byte)0b00001111, },
        new byte[] {(byte)0b00011111, },
        new byte[] {(byte)0b00111111, },
        new byte[] {(byte)0b01111111, },
        new byte[] {(byte)0b01111111, },
    };
    private static final int COLUMNS_MATRIX_8X8 = 8;
    private static final byte[][] COLUMN_MATRIX_8X8 = new byte[][] {
        new byte[] {(byte)0b00000000, },
        new byte[] {(byte)0b00000011, },
        new byte[] {(byte)0b00000111, },
        new byte[] {(byte)0b00001111, },
        new byte[] {(byte)0b00011111, },
        new byte[] {(byte)0b00111111, },
        new byte[] {(byte)0b01111111, },
        new byte[] {(byte)0b11111111, },
    };

    @Test
    public void test8X8() {
        testBitMatrix(ROWS_MATRIX_8X8, COLUMNS_MATRIX_8X8, ROW_MATRIX_8X8, COLUMN_MATRIX_8X8);
    }

    /**
     * (4 × 8)
     *                  1111
     *                  0111
     * 10000001         0011
     * 11000011   -->   0001
     * 11100111         0001
     * 11111111         0011
     *                  0111
     *                  1111
     */
    private static final int ROWS_MATRIX_4X8 = 4;
    private static final byte[][] ROW_MATRIX_4X8 = new byte[][] {
        new byte[] {(byte)0b10000001, },
        new byte[] {(byte)0b11000011, },
        new byte[] {(byte)0b11100111, },
        new byte[] {(byte)0b11111111, },
    };
    private static final int COLUMNS_MATRIX_4X8 = 8;
    private static final byte[][] COLUMN_MATRIX_4X8 = new byte[][] {
        new byte[] {(byte)0b00001111, },
        new byte[] {(byte)0b00000111, },
        new byte[] {(byte)0b00000011, },
        new byte[] {(byte)0b00000001, },
        new byte[] {(byte)0b00000001, },
        new byte[] {(byte)0b00000011, },
        new byte[] {(byte)0b00000111, },
        new byte[] {(byte)0b00001111, },
    };

    @Test
    public void test4X8() {
        testBitMatrix(ROWS_MATRIX_4X8, COLUMNS_MATRIX_4X8, ROW_MATRIX_4X8, COLUMN_MATRIX_4X8);
    }

    /**
     * (5 × 7)
     *                  01111
     * 0000000          00111
     * 1000000          00011
     * 1100000   -->    00001
     * 1110000          00000
     * 1111000          00000
     *                  00000
     */
    private static final int ROWS_MATRIX_5X7 = 5;
    private static final byte[][] ROW_MATRIX_5X7 = new byte[][] {
        new byte[] {0b00000000, },
        new byte[] {0b01000000, },
        new byte[] {0b01100000, },
        new byte[] {0b01110000, },
        new byte[] {0b01111000, },
    };
    private static final int COLUMNS_MATRIX_5X7 = 7;
    private static final byte[][] COLUMN_MATRIX_5X7 = new byte[][] {
        new byte[] {0b00001111, },
        new byte[] {0b00000111, },
        new byte[] {0b00000011, },
        new byte[] {0b00000001, },
        new byte[] {0b00000000, },
        new byte[] {0b00000000, },
        new byte[] {0b00000000, },
    };

    @Test
    public void test5X7() {
        testBitMatrix(ROWS_MATRIX_5X7, COLUMNS_MATRIX_5X7, ROW_MATRIX_5X7, COLUMN_MATRIX_5X7);
    }

    /**
     * (8 × 16)
     *
     * 0011111111111111     00000000
     * 0001111111111110     00000000
     * 0000111111111100     10000000
     * 0000011111111000     11000000
     * 0000001111110000 --> 11100000
     * 0000000111100000     11110000
     * 0000000011000000     11111000
     * 0000000001000000     11111100
     *                      11111110
     *                      11111111
     *                      11111100
     *                      11111000
     *                      11110000
     *                      11100000
     *                      11000000
     *                      10000000
     */
    private static final int ROWS_MATRIX_8X16 = 8;
    private static final byte[][] ROW_MATRIX_8X16 = new byte[][] {
        new byte[] { (byte)0b00111111, (byte)0b11111111, },
        new byte[] { (byte)0b00011111, (byte)0b11111110, },
        new byte[] { (byte)0b00001111, (byte)0b11111100, },
        new byte[] { (byte)0b00000111, (byte)0b11111000, },
        new byte[] { (byte)0b00000011, (byte)0b11110000, },
        new byte[] { (byte)0b00000001, (byte)0b11100000, },
        new byte[] { (byte)0b00000000, (byte)0b11000000, },
        new byte[] { (byte)0b00000000, (byte)0b01000000, },
    };
    private static final int COLUMNS_MATRIX_8X16 = 16;
    private static final byte[][] COLUMN_MATRIX_8X16 = new byte[][] {
        new byte[] { (byte)0b00000000, },
        new byte[] { (byte)0b00000000, },
        new byte[] { (byte)0b10000000, },
        new byte[] { (byte)0b11000000, },
        new byte[] { (byte)0b11100000, },
        new byte[] { (byte)0b11110000, },
        new byte[] { (byte)0b11111000, },
        new byte[] { (byte)0b11111100, },
        new byte[] { (byte)0b11111110, },
        new byte[] { (byte)0b11111111, },
        new byte[] { (byte)0b11111100, },
        new byte[] { (byte)0b11111000, },
        new byte[] { (byte)0b11110000, },
        new byte[] { (byte)0b11100000, },
        new byte[] { (byte)0b11000000, },
        new byte[] { (byte)0b10000000, },
    };

    @Test
    public void test8X16() {
        testBitMatrix(ROWS_MATRIX_8X16, COLUMNS_MATRIX_8X16, ROW_MATRIX_8X16, COLUMN_MATRIX_8X16);
    }

    /**
     * (1025 × 129)
     *
     *  100...000       111......111
     *  100...000       000......000
     *      .     -->       ...
     *      .           000......000
     *      .
     *      .
     *      .
     *      .
     *  100...000
     */
    private static final int ROWS_MATRIX_1025X129 = 1025;
    private static final byte[][] ROW_MATRIX_1025X129 = new byte[1025][];
    private static final int COLUMNS_MATRIX_1025X129 = 129;
    private static final byte[][] COLUMN_MATRIX_1025X129 = new byte[129][];
    static {
        IntStream.range(0, ROWS_MATRIX_1025X129).forEach(rowIndex -> {
            ROW_MATRIX_1025X129[rowIndex] = new byte[CommonUtils.getByteLength(COLUMNS_MATRIX_1025X129)];
            ROW_MATRIX_1025X129[rowIndex][0] = 0x01;
        });
        IntStream.range(0, COLUMNS_MATRIX_1025X129).forEach(columnIndex ->
            COLUMN_MATRIX_1025X129[columnIndex] = new byte[CommonUtils.getByteLength(ROWS_MATRIX_1025X129)]
        );
        Arrays.fill(COLUMN_MATRIX_1025X129[0], (byte)0xFF);
        BytesUtils.reduceByteArray(COLUMN_MATRIX_1025X129[0], ROWS_MATRIX_1025X129);
    }

    @Test
    public void test1025X129() {
        testBitMatrix(ROWS_MATRIX_1025X129, COLUMNS_MATRIX_1025X129, ROW_MATRIX_1025X129, COLUMN_MATRIX_1025X129);
    }

    private void testBitMatrix(int rows, int columns, byte[][] rowMatrix, byte[][] columnMatrix) {
        SimdTransBitMatrix a = SimdTransBitMatrixFactory.createInstance(type, rows, columns);
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            a.setColumn(columnIndex, columnMatrix[columnIndex]);
        }
        // transpose
        SimdTransBitMatrix b = a.transpose();
        for (int bColumnIndex = 0; bColumnIndex < rows; bColumnIndex++) {
            Assert.assertArrayEquals(rowMatrix[bColumnIndex], b.getColumn(bColumnIndex));
        }
        // transpose again
        SimdTransBitMatrix aPrime = b.transpose();
        for (int aColumnIndex = 0; aColumnIndex < columns; aColumnIndex++) {
            Assert.assertArrayEquals(columnMatrix[aColumnIndex], aPrime.getColumn(aColumnIndex));
        }
    }

    @Test
    public void testRandomBitMatrix() {
        testRandomBitMatrix(1, 1);
        testRandomBitMatrix(8, 8);
        testRandomBitMatrix(4, 8);
        testRandomBitMatrix(5, 7);
        testRandomBitMatrix(1025, 129);
        testRandomBitMatrix(1023, 127);
    }

    private void testRandomBitMatrix(int rows, int columns) {
        SimdTransBitMatrix a = SimdTransBitMatrixFactory.createInstance(type, rows, columns);
        int rowBytes = CommonUtils.getByteLength(rows);
        IntStream.range(0, columns).forEach(columnIndex -> {
            byte[] column = new byte[rowBytes];
            SECURE_RANDOM.nextBytes(column);
            BytesUtils.reduceByteArray(column, rows);
            a.setColumn(columnIndex, column);
        });
        // verify padding 0
        for (int aColumnIndex = 0; aColumnIndex < columns; aColumnIndex++) {
            Assert.assertTrue(BytesUtils.isReduceByteArray(a.getColumn(aColumnIndex), rows));
        }
        // transpose
        SimdTransBitMatrix b = a.transpose();
        // verify padding 0
        for (int bColumnIndex = 0; bColumnIndex < rows; bColumnIndex++) {
            Assert.assertTrue(BytesUtils.isReduceByteArray(b.getColumn(bColumnIndex), columns));
        }
        // verify equality
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                Assert.assertEquals(a.get(rowIndex, columnIndex), b.get(columnIndex, rowIndex));
            }
        }
        // transpose again
        SimdTransBitMatrix aPrime = b.transpose();
        // verify equality
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            Assert.assertArrayEquals(a.getColumn(columnIndex), aPrime.getColumn(columnIndex));
        }
    }
}
