package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 用byte[]表示的布尔方阵。
 *
 * @author Weiran Liu
 * @date 2021/12/20
 */
public class ByteSquareDenseBitMatrix implements SquareDenseBitMatrix {
    /**
     * 布尔方阵的大小
     */
    private int size;
    /**
     * 布尔方阵的字节大小
     */
    private int byteSize;
    /**
     * 偏移量
     */
    private int offset;
    /**
     * 布尔方阵
     */
    private byte[][] byteBitMatrix;

    /**
     * 构建布尔方阵。
     *
     * @param positions 布尔方阵中取值为1的位置。
     */
    public static ByteSquareDenseBitMatrix fromSparse(int[][] positions) {
        ByteSquareDenseBitMatrix squareDenseBitMatrix = new ByteSquareDenseBitMatrix();
        assert positions.length > 0 : "size must be greater than 0";
        squareDenseBitMatrix.size = positions.length;
        squareDenseBitMatrix.byteSize = CommonUtils.getByteLength(squareDenseBitMatrix.size);
        squareDenseBitMatrix.offset = squareDenseBitMatrix.byteSize * Byte.SIZE - squareDenseBitMatrix.size;
        squareDenseBitMatrix.byteBitMatrix = Arrays.stream(positions)
            .map(rowPositions -> {
                byte[] row = new byte[squareDenseBitMatrix.byteSize];
                for (int position : rowPositions) {
                    assert position >= 0 && position < squareDenseBitMatrix.size
                        : "position must be in range [0, " + squareDenseBitMatrix.size + "): " + position;
                    // 将每个所需的位置设置为1
                    BinaryUtils.setBoolean(row, position + squareDenseBitMatrix.offset, true);
                }
                return row;
            })
            .toArray(byte[][]::new);
        return squareDenseBitMatrix;
    }

    /**
     * 构建布尔方阵。
     *
     * @param bitMatrix 布尔方阵描述。
     */
    public static ByteSquareDenseBitMatrix fromDense(byte[][] bitMatrix) {
        ByteSquareDenseBitMatrix squareDenseBitMatrix = new ByteSquareDenseBitMatrix();
        assert bitMatrix.length > 0 : "size must be greater than 0";
        squareDenseBitMatrix.size = bitMatrix.length;
        squareDenseBitMatrix.byteSize = CommonUtils.getByteLength(squareDenseBitMatrix.size);
        squareDenseBitMatrix.offset = squareDenseBitMatrix.byteSize * Byte.SIZE - squareDenseBitMatrix.size;
        squareDenseBitMatrix.byteBitMatrix = Arrays.stream(bitMatrix)
            .peek(row -> {
                assert row.length == squareDenseBitMatrix.byteSize
                    : "row byte length must be " + squareDenseBitMatrix.byteSize + ": " + row.length;
                assert BytesUtils.isReduceByteArray(row, squareDenseBitMatrix.size)
                    : "row must contain " + squareDenseBitMatrix.size + "valid bits, current row: " + Hex.toHexString(row);
            })
            .toArray(byte[][]::new);
        return squareDenseBitMatrix;
    }

    /**
     * 私有构造函数。
     */
    private ByteSquareDenseBitMatrix() {
        // empty
    }

    @Override
    public SquareDenseBitMatrix add(DenseBitMatrix that) {
        assert size == that.getRows() : "input matrix must have " + this.size + " rows: " + that.getRows();
        assert size == that.getColumns() : "input matrix must have " + this.size + " columns: " + that.getColumns();
        byte[][] addByteBitMatrix = IntStream.range(0, size)
            .mapToObj(x -> BytesUtils.xor(byteBitMatrix[x], that.getRow(x)))
            .toArray(byte[][]::new);
        return ByteSquareDenseBitMatrix.fromDense(addByteBitMatrix);
    }

    @Override
    public void addi(DenseBitMatrix that) {
        assert size == that.getRows() : "input matrix must have " + size + " rows: " + that.getRows();
        assert size == that.getColumns() : "input matrix must have " + size + " columns: " + that.getColumns();
        IntStream.range(0, size).forEach(x -> BytesUtils.xori(byteBitMatrix[x], that.getRow(x)));
    }


    @Override
    public DenseBitMatrix multiply(DenseBitMatrix that) {
        assert size == that.getRows() : "input matrix must have " + size + " rows: " + that.getRows();
        int mulColumns = that.getColumns();
        int mulByteColumns = CommonUtils.getByteLength(mulColumns);
        byte[][] mulByteBitMatrix = new byte[size][mulByteColumns];
        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            byte[] input = byteBitMatrix[rowIndex];
            for (int columnIndex = 0; columnIndex < size; columnIndex++) {
                if (BinaryUtils.getBoolean(input, columnIndex + offset)) {
                    BytesUtils.xori(mulByteBitMatrix[rowIndex], that.getRow(columnIndex));
                }
            }
        }
        if (size == mulColumns) {
            return ByteSquareDenseBitMatrix.fromDense(mulByteBitMatrix);
        } else {
            return ByteDenseBitMatrix.fromDense(mulColumns, mulByteBitMatrix);
        }
    }

    @Override
    public byte[] lmul(final byte[] v) {
        assert v.length == byteSize : "byte length of v must be " + byteSize + ": " + v.length;
        assert BytesUtils.isReduceByteArray(v, size)
            : "v must contain " + size + " valid bits, current v: " + Hex.toHexString(v);
        byte[] output = new byte[byteSize];
        for (int y = 0; y < size; y++) {
            if (BinaryUtils.getBoolean(v, y + offset)) {
                BytesUtils.xori(output, byteBitMatrix[y]);
            }
        }
        return output;
    }

    @Override
    public boolean[] lmul(boolean[] v) {
        assert v.length == size : "length of v must be " + size + ": " + v.length;
        byte[] output = new byte[byteSize];
        for (int y = 0; y < size; y++) {
            if (v[y]) {
                BytesUtils.xori(output, byteBitMatrix[y]);
            }
        }
        return BinaryUtils.byteArrayToBinary(output, size);
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
            if (BinaryUtils.getBoolean(v, rowIndex + offset)) {
                BytesUtils.xori(t, byteBitMatrix[rowIndex]);
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
                    t[columnIndex] ^= BinaryUtils.getBoolean(byteBitMatrix[rowIndex], columnIndex + offset);
                }
            }
        }
    }

    @Override
    public SquareDenseBitMatrix transpose(EnvType envType, boolean parallel) {
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
        return ByteSquareDenseBitMatrix.fromDense(transByteBitMatrix);
    }

    @Override
    public SquareDenseBitMatrix inverse() {
        // 构造布尔矩阵
        boolean[][] matrix = Arrays.stream(byteBitMatrix)
            .map(row -> BinaryUtils.byteArrayToBinary(row, size))
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
        byte[][] invertByteBitMatrix = Arrays.stream(inverseMatrix)
            .map(BinaryUtils::binaryToRoundByteArray)
            .toArray(byte[][]::new);
        return ByteSquareDenseBitMatrix.fromDense(invertByteBitMatrix);
    }

    @Override
    public int getRows() {
        return size;
    }

    @Override
    public byte[] getRow(int x) {
        return byteBitMatrix[x];
    }

    @Override
    public int getColumns() {
        return size;
    }

    @Override
    public boolean get(int x, int y) {
        assert y >= 0 && y < size : "y must be in range [0, " + size + "): " + y;
        return BinaryUtils.getBoolean(byteBitMatrix[x], y + offset);
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getByteSize() {
        return byteSize;
    }

    @Override
    public byte[][] toByteArrays() {
        return byteBitMatrix;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(byteBitMatrix).forEach(row -> {
            BigInteger rowBigInteger = new BigInteger(1, row);
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
        if (!(obj instanceof ByteSquareDenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ByteSquareDenseBitMatrix that = (ByteSquareDenseBitMatrix) obj;
        return new EqualsBuilder().append(this.byteBitMatrix, that.byteBitMatrix).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(byteBitMatrix).toHashCode();
    }
}
