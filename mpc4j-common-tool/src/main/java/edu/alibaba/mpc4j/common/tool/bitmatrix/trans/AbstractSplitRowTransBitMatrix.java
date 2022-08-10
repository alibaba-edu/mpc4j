package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 将矩阵按行分块，每块为column列、2^10行，转置时按照分块转置。libOTe代码中使用了此方法，很容易并行化处理。
 *
 * @author Weiran Liu
 * @date 2021/01/25
 */
abstract class AbstractSplitRowTransBitMatrix extends AbstractTransBitMatrix {
    /**
     * 每个分块所包含的行数量，根据libOTe的参数设置，以2^10 = 1024为一个单位。
     */
    private static final int ROWS_PER_BLOCK = 1 << 10;
    /**
     * 每个分块包含行数量所对应的字节数量
     */
    private static final int ROW_BYTES_PER_BLOCK = CommonUtils.getByteLength(ROWS_PER_BLOCK);
    /**
     * 字节行数
     */
    private final int rowBytes;
    /**
     * 分块数量
     */
    private final int blockNum;
    /**
     * 分快行数
     */
    private final int rowBlockBytes;
    /**
     * 列偏移量
     */
    private final int offset;
    /**
     * 字节列数
     */
    private final int columnBytes;
    /**
     * 分块矩阵
     */
    private final TransBitMatrix[] blockData;
    /**
     * 是否为转置表示
     */
    private boolean isTransposed;

    AbstractSplitRowTransBitMatrix(TransBitMatrixType transBitMatrixType, int rows, int columns) {
        super(rows, columns);
        rowBytes = CommonUtils.getByteLength(rows);
        blockNum = (ROWS_PER_BLOCK + rows - 1) / ROWS_PER_BLOCK;
        rowBlockBytes = blockNum * ROW_BYTES_PER_BLOCK;
        offset = blockNum * ROWS_PER_BLOCK - rows;
        columnBytes = CommonUtils.getByteLength(columns);
        blockData = new TransBitMatrix[blockNum];
        // 分别构建布尔矩阵
        IntStream.range(0, blockNum).forEach(blockIndex ->
            blockData[blockIndex] = TransBitMatrixFactory.createInstance(transBitMatrixType, ROWS_PER_BLOCK, columns)
        );
        this.isTransposed = false;
    }

    @Override
    public boolean get(int x, int y) {
        if (!isTransposed) {
            // 尚未转置，将行索引值补足后，取对应分块的布尔值
            assert (x >= 0 && x < rows);
            assert (y >= 0 && y < columns);
            int actualX = x + offset;
            return this.blockData[actualX / ROWS_PER_BLOCK].get(actualX % ROWS_PER_BLOCK, y);
        } else {
            // 转置，将列索引值补足后，取对应分块的布尔值
            assert (x >= 0 && x < columns);
            assert (y >= 0 && y < rows);
            int actualY = y + offset;
            return blockData[actualY / ROWS_PER_BLOCK].get(x, actualY % ROWS_PER_BLOCK);
        }
    }

    @Override
    public byte[] getColumn(int y) {
        if (!isTransposed) {
            assert (y >= 0 && y < columns);
            // 尚未转置，将各个分块的数据拼接到一起
            ByteBuffer byteBuffer = ByteBuffer.allocate(rowBlockBytes);
            // 按顺序处理，不能并行
            Arrays.stream(blockData).forEach(block -> byteBuffer.put(block.getColumn(y)));
            byte[] columnBlockByteArray = byteBuffer.array();
            byte[] columnByteArray = new byte[rowBytes];
            System.arraycopy(
                columnBlockByteArray, rowBlockBytes - rowBytes, columnByteArray, 0, rowBytes
            );
            // 要将最开始的complementNum行修剪为0
            BytesUtils.reduceByteArray(columnByteArray, rows);
            return columnByteArray;
        } else {
            assert (y >= 0 && y < rows);
            // 转置完毕，取对应的列返回
            int actualColumn = y + offset;
            return blockData[actualColumn / ROWS_PER_BLOCK].getColumn(actualColumn % ROWS_PER_BLOCK);
        }
    }

    @Override
    public void setColumn(int y, byte[] byteArray) {
        if (!isTransposed) {
            assert (y >= 0 && y < columns);
            assert byteArray.length == rowBytes;
            assert BytesUtils.isReduceByteArray(byteArray, rows);
            // 尚未转置，把数据拆分到各个分块中
            byte[] columnBlockArray = new byte[rowBlockBytes];
            System.arraycopy(
                byteArray, 0, columnBlockArray, rowBlockBytes - rowBytes, rowBytes
            );
            // 按顺序处理，不能并行
            IntStream.range(0, blockNum).forEach(blockIndex -> {
                byte[] blockColumnByteArray = Arrays.copyOfRange(
                    columnBlockArray, blockIndex * ROW_BYTES_PER_BLOCK, (blockIndex + 1) * ROW_BYTES_PER_BLOCK
                );
                blockData[blockIndex].setColumn(y, blockColumnByteArray);
            });
        } else {
            assert (y >= 0 && y < rows);
            assert byteArray.length == columnBytes;
            assert BytesUtils.isReduceByteArray(byteArray, columns);
            // 已经转置，直接设置对应分组的对应列
            int actualColumn = y + offset;
            blockData[actualColumn / ROWS_PER_BLOCK].setColumn(actualColumn % ROWS_PER_BLOCK, byteArray);
        }
    }

    @Override
    public int getRows() {
        return this.isTransposed ? columns : rows;
    }

    @Override
    public int getColumns() {
        return this.isTransposed ? rows : columns;
    }

    @Override
    public TransBitMatrix transpose() {
        AbstractSplitRowTransBitMatrix b = (AbstractSplitRowTransBitMatrix) TransBitMatrixFactory.createInstance(
            getTransBitMatrixType(), rows, columns
        );
        b.isTransposed = !this.isTransposed;
        // 并行处理转置
        IntStream.range(0, this.blockNum).parallel().forEach(
            blockIndex -> b.blockData[blockIndex] = this.blockData[blockIndex].transpose()
        );
        return b;
    }
}
