package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 将矩阵按列分块，每块为2^10列、row行，转置时按照分块转置，适用于行比较少、列比较多的并行转置布尔矩阵。
 *
 * @author Weiran Liu
 * @date 2021/11/30
 */
abstract class AbstractSplitColTransBitMatrix extends AbstractTransBitMatrix {
    /**
     * 每个分块所包含的行数量，根据libOTe的参数设置，以2^10 = 1024为一个单位。
     */
    private static final int COLUMNS_PER_BLOCK = 1 << 10;
    /**
     * 每个分块包含行数量所对应的字节数量
     */
    private static final int COLUMNS_BYTES_PER_BLOCK = CommonUtils.getByteLength(COLUMNS_PER_BLOCK);
    /**
     * 字节行数
     */
    private final int rowBytes;
    /**
     * 字节列数
     */
    private final int columnBytes;
    /**
     * 分块数量
     */
    private final int blockNum;
    /**
     * 分快列数
     */
    private final int columnBlockBytes;
    /**
     * 列偏移量
     */
    private final int offset;
    /**
     * 分块矩阵
     */
    private final TransBitMatrix[] blockData;
    /**
     * 是否为转置表示
     */
    private boolean isTransposed;

    AbstractSplitColTransBitMatrix(TransBitMatrixType transBitMatrixType, int rows, int columns) {
        super(rows, columns);
        rowBytes = CommonUtils.getByteLength(rows);
        columnBytes = CommonUtils.getByteLength(columns);
        blockNum = (COLUMNS_PER_BLOCK + columns - 1) / COLUMNS_PER_BLOCK;
        columnBlockBytes = blockNum * COLUMNS_BYTES_PER_BLOCK;
        offset = blockNum * COLUMNS_PER_BLOCK - columns;
        blockData = new TransBitMatrix[blockNum];
        // 分别构建布尔矩阵
        IntStream.range(0, blockNum).forEach(blockIndex ->
            blockData[blockIndex] = TransBitMatrixFactory.createInstance(transBitMatrixType, rows, COLUMNS_PER_BLOCK)
        );
        this.isTransposed = false;
    }

    @Override
    public boolean get(int x, int y) {
        if (!isTransposed) {
            // 尚未转置，将列索引值补足后，取对应分块的布尔值
            assert (x >= 0 && x < rows);
            assert (y >= 0 && y < columns);
            int actualY = y + offset;
            return this.blockData[actualY / COLUMNS_PER_BLOCK].get(x, actualY % COLUMNS_PER_BLOCK);
        } else {
            // 转置，将行索引值补足后，取对应分块的布尔值
            assert (x >= 0 && x < columns);
            assert (y >= 0 && y < rows);
            int actualX = x + offset;
            return blockData[actualX / COLUMNS_PER_BLOCK].get(actualX % COLUMNS_PER_BLOCK, y);
        }
    }

    @Override
    public byte[] getColumn(int y) {
        if (!isTransposed) {
            assert (y >= 0 && y < columns);
            // 尚未转置，取对应的列返回
            int actualColumn = y + offset;
            return this.blockData[actualColumn / COLUMNS_PER_BLOCK].getColumn(actualColumn % COLUMNS_PER_BLOCK);
        } else {
            assert (y >= 0 && y < rows);
            // 转置完毕，将各个分块的数据拼接到一起
            ByteBuffer byteBuffer = ByteBuffer.allocate(columnBlockBytes);
            // 按顺序处理，不能并行
            Arrays.stream(this.blockData).forEach(block -> byteBuffer.put(block.getColumn(y)));
            byte[] columnBlockByteArray = byteBuffer.array();
            byte[] columnByteArray = new byte[columnBytes];
            System.arraycopy(
                columnBlockByteArray, columnBlockBytes - columnBytes, columnByteArray, 0, columnBytes
            );
            // 要将最开始的complementNum行修剪为0
            BytesUtils.reduceByteArray(columnByteArray, columns);
            return columnByteArray;
        }
    }

    @Override
    public void setColumn(int y, byte[] byteArray) {
        if (!isTransposed) {
            assert (y >= 0 && y < columns);
            assert (byteArray.length == rowBytes);
            assert BytesUtils.isReduceByteArray(byteArray, rows);
            // 尚未转置，直接设置对应分组的对应列
            int actualColumn = y + offset;
            this.blockData[actualColumn / COLUMNS_PER_BLOCK].setColumn(actualColumn % COLUMNS_PER_BLOCK,
                byteArray);
        } else {
            assert (y >= 0 && y < rows);
            assert byteArray.length == columnBytes;
            assert BytesUtils.isReduceByteArray(byteArray, columns);
            // 已经转置，把数据拆分到各个分块中
            byte[] columnBlockArray = new byte[columnBlockBytes];
            System.arraycopy(
                byteArray, 0, columnBlockArray, columnBlockBytes - columnBytes, columnBytes
            );
            // 按顺序处理，不能并行
            IntStream.range(0, this.blockNum).forEach(blockIndex -> {
                byte[] blockColumnByteArray = Arrays.copyOfRange(
                    columnBlockArray, blockIndex * COLUMNS_BYTES_PER_BLOCK, (blockIndex + 1) * COLUMNS_BYTES_PER_BLOCK
                );
                this.blockData[blockIndex].setColumn(y, blockColumnByteArray);
            });
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
        AbstractSplitColTransBitMatrix b = (AbstractSplitColTransBitMatrix) TransBitMatrixFactory.createInstance(
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
