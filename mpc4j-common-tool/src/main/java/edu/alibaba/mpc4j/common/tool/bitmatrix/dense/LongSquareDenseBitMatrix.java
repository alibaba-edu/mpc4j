package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 用long[]维护的布尔方阵。
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
public class LongSquareDenseBitMatrix implements SquareDenseBitMatrix {
    /**
     * 布尔方阵的大小
     */
    private int size;
    /**
     * 布尔方阵的字节大小
     */
    private int byteSize;
    /**
     * 字节偏移量
     */
    private int byteOffset;
    /**
     * 布尔方阵的长整数大小
     */
    private int longSize;
    /**
     * 长整数偏移量
     */
    private int longOffset;
    /**
     * 布尔方阵
     */
    private long[][] longBitMatrix;

    /**
     * 构建布尔方阵。
     *
     * @param positions 布尔方阵中取值为1的位置。
     */
    public static LongSquareDenseBitMatrix fromSparse(int[][] positions) {
        LongSquareDenseBitMatrix squareDenseBitMatrix = new LongSquareDenseBitMatrix();
        assert positions.length > 0 : "size must be greater than 0";
        squareDenseBitMatrix.size = positions.length;
        squareDenseBitMatrix.byteSize = CommonUtils.getByteLength(squareDenseBitMatrix.size);
        squareDenseBitMatrix.byteOffset = squareDenseBitMatrix.byteSize * Byte.SIZE - squareDenseBitMatrix.size;
        squareDenseBitMatrix.longSize = CommonUtils.getLongLength(squareDenseBitMatrix.size);
        squareDenseBitMatrix.longOffset = squareDenseBitMatrix.longSize * Long.SIZE - squareDenseBitMatrix.size;
        squareDenseBitMatrix.longBitMatrix = Arrays.stream(positions)
            .map(rowPositions -> {
                long[] row = new long[squareDenseBitMatrix.longSize];
                for (int position : rowPositions) {
                    assert position >= 0 && position < squareDenseBitMatrix.size
                        : "position must be in range [0, " + squareDenseBitMatrix.size + "): " + position;
                    // 将每个所需的位置设置为1
                    BinaryUtils.setBoolean(row, position + squareDenseBitMatrix.longOffset, true);
                }
                return row;
            })
            .toArray(long[][]::new);
        return squareDenseBitMatrix;
    }

    /**
     * 构建布尔方阵。
     *
     * @param bitMatrix 布尔方阵描述。
     */
    public static LongSquareDenseBitMatrix fromDense(byte[][] bitMatrix) {
        LongSquareDenseBitMatrix squareDenseBitMatrix = new LongSquareDenseBitMatrix();
        assert bitMatrix.length > 0 : "size must be greater than 0";
        squareDenseBitMatrix.size = bitMatrix.length;
        squareDenseBitMatrix.byteSize = CommonUtils.getByteLength(squareDenseBitMatrix.size);
        squareDenseBitMatrix.byteOffset = squareDenseBitMatrix.byteSize * Byte.SIZE - squareDenseBitMatrix.size;
        squareDenseBitMatrix.longSize = CommonUtils.getLongLength(squareDenseBitMatrix.size);
        squareDenseBitMatrix.longOffset = squareDenseBitMatrix.longSize * Long.SIZE - squareDenseBitMatrix.size;
        int byteSize = CommonUtils.getByteLength(squareDenseBitMatrix.size);
        squareDenseBitMatrix.longBitMatrix = Arrays.stream(bitMatrix)
            .map(row -> {
                assert row.length == byteSize : "row byte length must be " + byteSize + ": " + row.length;
                assert BytesUtils.isReduceByteArray(row, squareDenseBitMatrix.size)
                    : "row must contain " + squareDenseBitMatrix.size + "valid bits, current row: " + Hex.toHexString(row);
                return LongUtils.byteArrayToRoundLongArray(row);
            })
            .toArray(long[][]::new);
        return squareDenseBitMatrix;
    }

    /**
     * 构建布尔方阵。
     *
     * @param bitMatrix 布尔方阵描述。
     */
    private static LongSquareDenseBitMatrix fromDense(long[][] bitMatrix) {
        LongSquareDenseBitMatrix squareDenseBitMatrix = new LongSquareDenseBitMatrix();
        assert bitMatrix.length > 0 : "size must be greater than 0";
        squareDenseBitMatrix.size = bitMatrix.length;
        squareDenseBitMatrix.byteSize = CommonUtils.getByteLength(squareDenseBitMatrix.size);
        squareDenseBitMatrix.byteOffset = squareDenseBitMatrix.byteSize * Byte.SIZE - squareDenseBitMatrix.size;
        squareDenseBitMatrix.longSize = CommonUtils.getLongLength(squareDenseBitMatrix.size);
        squareDenseBitMatrix.longOffset = squareDenseBitMatrix.longSize * Long.SIZE - squareDenseBitMatrix.size;
        squareDenseBitMatrix.longBitMatrix = Arrays.stream(bitMatrix)
            .peek(row -> {
                assert row.length == squareDenseBitMatrix.longSize
                    : "row byte length must be " + squareDenseBitMatrix.longSize + ": " + row.length;
                assert LongUtils.isReduceLongArray(row, squareDenseBitMatrix.size)
                    : "row must contain " + squareDenseBitMatrix.size + "valid bits";
            })
            .toArray(long[][]::new);
        return squareDenseBitMatrix;
    }

    /**
     * 私有构造函数。
     */
    private LongSquareDenseBitMatrix() {
        // empty
    }

    @Override
    public SquareDenseBitMatrix add(DenseBitMatrix that) {
        assert size == that.getRows() : "input matrix must have " + this.size + " rows: " + that.getRows();
        assert size == that.getColumns() : "input matrix must have " + this.size + " columns: " + that.getColumns();
        long[][] addLongBitMatrix = IntStream.range(0, size)
            .mapToObj(x -> LongUtils.xor(longBitMatrix[x], LongUtils.byteArrayToRoundLongArray(that.getRow(x))))
            .toArray(long[][]::new);
        return LongSquareDenseBitMatrix.fromDense(addLongBitMatrix);
    }

    @Override
    public void addi(DenseBitMatrix that) {
        assert size == that.getRows() : "input matrix must have " + this.size + " rows: " + that.getRows();
        assert size == that.getColumns() : "input matrix must have " + this.size + " columns: " + that.getColumns();
        IntStream.range(0, size).forEach(x ->
            LongUtils.xori(longBitMatrix[x], LongUtils.byteArrayToRoundLongArray(that.getRow(x)))
        );
    }

    @Override
    public DenseBitMatrix multiply(DenseBitMatrix that) {
        assert size == that.getRows() : "input matrix must have " + size + " rows: " + that.getRows();
        if (that instanceof LongSquareDenseBitMatrix) {
            // LongSquareDenseBitMatrix，特殊处理
            LongSquareDenseBitMatrix thatLong = (LongSquareDenseBitMatrix) that;
            long[][] mulLongBitMatrix = new long[size][longSize];
            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                long[] input = longBitMatrix[rowIndex];
                for (int columnIndex = 0; columnIndex < size; columnIndex++) {
                    if (BinaryUtils.getBoolean(input, columnIndex + longOffset)) {
                        LongUtils.xori(mulLongBitMatrix[rowIndex], thatLong.longBitMatrix[columnIndex]);
                    }
                }
            }
            return LongSquareDenseBitMatrix.fromDense(mulLongBitMatrix);
        }
        int mulColumns = that.getColumns();
        if (size == mulColumns) {
            // 方阵乘以方阵，特殊处理
            int mulLongColumns = CommonUtils.getLongLength(mulColumns);
            long[][] mulLongBitMatrix = new long[size][mulLongColumns];
            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                long[] input = longBitMatrix[rowIndex];
                for (int columnIndex = 0; columnIndex < size; columnIndex++) {
                    if (BinaryUtils.getBoolean(input, columnIndex + longOffset)) {
                        long[] row = LongUtils.byteArrayToRoundLongArray(that.getRow(columnIndex));
                        LongUtils.xori(mulLongBitMatrix[rowIndex], row);
                    }
                }
            }
            return LongSquareDenseBitMatrix.fromDense(mulLongBitMatrix);
        } else {
            // 正常处理
            int mulByteColumns = CommonUtils.getByteLength(mulColumns);
            byte[][] mulByteBitMatrix = new byte[size][mulByteColumns];
            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                long[] input = longBitMatrix[rowIndex];
                for (int columnIndex = 0; columnIndex < size; columnIndex++) {
                    if (BinaryUtils.getBoolean(input, columnIndex + longOffset)) {
                        BytesUtils.xori(mulByteBitMatrix[rowIndex], that.getRow(columnIndex));
                    }
                }
            }
            return ByteDenseBitMatrix.fromDense(mulColumns, mulByteBitMatrix);
        }
    }

    @Override
    public byte[] lmul(final byte[] v) {
        assert v.length == byteSize : "input.length must be equal to " + byteSize + ": " + v.length;
        assert BytesUtils.isReduceByteArray(v, size);
        int byteArrayOffset = v.length * Byte.SIZE - size;
        long[] longOutput = new long[longSize];
        for (int y = 0; y < size; y++) {
            if (BinaryUtils.getBoolean(v, y + byteArrayOffset)) {
                LongUtils.xori(longOutput, longBitMatrix[y]);
            }
        }
        return LongUtils.longArrayToByteArray(longOutput, byteSize);
    }

    @Override
    public boolean[] lmul(boolean[] v) {
        assert v.length == size : "length of v must be " + size + ": " + v.length;
        long[] output = new long[longSize];
        for (int y = 0; y < size; y++) {
            if (v[y]) {
                LongUtils.xori(output, longBitMatrix[y]);
            }
        }
        return BinaryUtils.longArrayToBinary(output, size);
    }

    @Override
    public byte[][] lExtMul(byte[][] v) {
        assert v.length == size : "length of v must be " + size + ": " + v.length;
        byte[][] output = new byte[size][v[0].length];
        lExtMulAddi(v, output);
        return output;
    }

    @Override
    public void lmulAddi(byte[] v, byte[] t) {
        assert v.length == byteSize : "byte length of v must be " + byteSize + ": " + v.length;
        assert BytesUtils.isReduceByteArray(v, size)
            : "v must contain " + size + " valid bits, current v: " + Hex.toHexString(v);
        assert t.length == byteSize : "byte length of t must be " + byteSize + ": " + t.length;
        assert BytesUtils.isReduceByteArray(t, size)
            : "t must contain " + size + " valid bits, current t: " + Hex.toHexString(v);
        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            if (BinaryUtils.getBoolean(v, rowIndex + byteOffset)) {
                BytesUtils.xori(t, LongUtils.longArrayToByteArray(longBitMatrix[rowIndex], byteSize));
            }
        }
    }

    @Override
    public void lmulAddi(boolean[] v, boolean[] t) {
        assert v.length == size : "length of v must be " + size + ": " + v.length;
        assert t.length == size :  "length of t must be " + size + ": " + t.length;
        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            if (v[rowIndex]) {
                for (int columnIndex = 0; columnIndex < size; columnIndex++) {
                    t[columnIndex] ^= BinaryUtils.getBoolean(longBitMatrix[rowIndex], columnIndex + longOffset);
                }
            }
        }
    }

    @Override
    public void lExtMulAddi(byte[][] v, byte[][] t) {
        assert v.length == size : "length of v must be " + size + ": " + v.length;
        assert t.length == size :  "length of t must be " + size + ": " + t.length;
        assert v[0].length == t[0].length : "length of t[i] must be " + v[0].length + ": " + t[0].length;
        for (int columnIndex = 0; columnIndex < size; columnIndex++) {
            for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                if (BinaryUtils.getBoolean(longBitMatrix[rowIndex], columnIndex + longOffset)) {
                    BytesUtils.xori(t[columnIndex], v[rowIndex]);
                }
            }
        }
    }


    /**
     * 当前布尔矩阵左乘向量，即计算v·M。
     *
     * @param v 向量。
     * @return 左乘结果。
     */
    public long[] lmul(final long[] v) {
        assert v.length == longSize;
        assert LongUtils.isReduceLongArray(v, size);
        long[] longOutput = new long[longSize];
        for (int y = 0; y < size; y++) {
            if (BinaryUtils.getBoolean(v, y + longOffset)) {
                LongUtils.xori(longOutput, longBitMatrix[y]);
            }
        }
        return longOutput;
    }

    @Override
    public SquareDenseBitMatrix transpose(EnvType envType, boolean parallel) {
        byte[][] byteBitMatrix = Arrays.stream(longBitMatrix)
            .map(longRow -> LongUtils.longArrayToByteArray(longRow, byteSize))
            .toArray(byte[][]::new);
        // 调用TransBitMatrix实现转置，TransBitMatrix是按列表示的，所以设置时候要反过来
        TransBitMatrix originTransBitMatrix = TransBitMatrixFactory.createInstance(envType, size, size, parallel);
        for (int sizeIndex = 0; sizeIndex < size; sizeIndex++) {
            originTransBitMatrix.setColumn(sizeIndex, byteBitMatrix[sizeIndex]);
        }
        TransBitMatrix transposedTransBitMatrix = originTransBitMatrix.transpose();
        byte[][] transByteBitMatrix = new byte[size][byteSize];
        for (int transSizeIndex = 0; transSizeIndex < size; transSizeIndex++) {
            transByteBitMatrix[transSizeIndex] = transposedTransBitMatrix.getColumn(transSizeIndex);
        }
        return LongSquareDenseBitMatrix.fromDense(transByteBitMatrix);
    }

    @Override
    public SquareDenseBitMatrix inverse() {
        // 构造布尔矩阵
        boolean[][] matrix = Arrays.stream(longBitMatrix)
            .map(x -> BinaryUtils.longArrayToBinary(x, size))
            .toArray(boolean[][]::new);
        // 构造逆矩阵，先将逆矩阵初始化为单位阵
        boolean[][] inverseMatrix = new boolean[size][size];
        IntStream.range(0, size).forEach(i -> inverseMatrix[i][i] = true);
        // 利用初等变换计算逆矩阵。首先将左矩阵转换为上三角矩阵
        for (int p = 0; p < size; p++) {
            if (!matrix[p][p]) {
                // 找到一个不为0的行
                int other = p + 1;
                while (other < size && !matrix[other][p]) {
                    other++;
                }
                if (other >= size) {
                    throw new ArithmeticException("Cannot invert bit matrix");
                } else {
                    // 左侧矩阵行swap
                    boolean[] matrixRowTemp = matrix[p];
                    matrix[p] = matrix[other];
                    matrix[other] = matrixRowTemp;
                    // 右侧矩阵行swap
                    boolean[] inverseMatrixRowTemp = inverseMatrix[p];
                    inverseMatrix[p] = inverseMatrix[other];
                    inverseMatrix[other] = inverseMatrixRowTemp;
                }
            }
            // 左右侧矩阵高斯消元
            for (int i = p + 1; i < size; i++) {
                if (matrix[i][p]) {
                    for (int j = 0; j < size; j++) {
                        matrix[i][j] = matrix[i][j] ^ matrix[p][j];
                        inverseMatrix[i][j] = inverseMatrix[i][j] ^ inverseMatrix[p][j];
                    }
                }
            }
        }
        // 将左侧矩阵转为单位矩阵，此时右侧得到的矩阵就是左侧矩阵的逆矩阵
        for (int p = size - 1; p >= 0; p--) {
            for (int r = 0; r < p; r++) {
                if (matrix[r][p]) {
                    // 如果有1的，则进行相加
                    for (int j = 0; j < size; j++) {
                        matrix[r][j] ^= matrix[p][j];
                        inverseMatrix[r][j] ^= inverseMatrix[p][j];
                    }
                }
            }
        }
        // 返回逆矩阵
        long[][] invertLongBitMatrix = Arrays.stream(inverseMatrix)
            .map(BinaryUtils::binaryToRoundLongArray)
            .toArray(long[][]::new);
        return LongSquareDenseBitMatrix.fromDense(invertLongBitMatrix);
    }

    @Override
    public int getRows() {
        return size;
    }

    @Override
    public byte[] getRow(int x) {
        return LongUtils.longArrayToByteArray(longBitMatrix[x], byteSize);
    }

    @Override
    public int getColumns() {
        return size;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getByteSize() {
        return byteSize;
    }

    /**
     * 返回布尔方阵的长整数大小。
     *
     * @return 布尔方阵的长整数大小。
     */
    public int getLongSize() {
        return longSize;
    }

    @Override
    public boolean get(int x, int y) {
        assert y >= 0 && y < size : "y must be in range [0, " + size + "): " + y;
        return BinaryUtils.getBoolean(longBitMatrix[x], y + longOffset);
    }

    @Override
    public byte[][] toByteArrays() {
        return Arrays.stream(longBitMatrix)
            .map(longRow -> LongUtils.longArrayToByteArray(longRow, byteSize))
            .toArray(byte[][]::new);
    }

    /**
     * 返回表示矩阵的长整数数组。
     *
     * @return 表示矩阵的长整数数组。
     */
    public long[][] toLongArrays() {
        return longBitMatrix;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(longBitMatrix).forEach(longRow -> {
            BigInteger rowBigInteger = new BigInteger(1, LongUtils.longArrayToByteArray(longRow));
            StringBuilder rowStringBuilder = new StringBuilder(rowBigInteger.toString(2));
            while (rowStringBuilder.length() < size) {
                rowStringBuilder.insert(0, "0");
            }
            stringBuilder.append(rowStringBuilder).append("\n");
        });
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LongSquareDenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        LongSquareDenseBitMatrix that = (LongSquareDenseBitMatrix) obj;
        return new EqualsBuilder().append(this.longBitMatrix, that.longBitMatrix).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(longBitMatrix).toHashCode();
    }
}
