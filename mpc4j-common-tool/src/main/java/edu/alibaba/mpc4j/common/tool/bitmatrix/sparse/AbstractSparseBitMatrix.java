package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteSquareDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 稀疏布尔矩阵抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/09/21
 */
public abstract class AbstractSparseBitMatrix {
    /**
     * 按列存储稀疏矩阵中值为1的点的位置
     * 例如，若colsList.get(0) = [1,4,7]，表示矩阵第0列的第1、4、7个元素为1,该列其余元素为0。
     */
    protected ArrayList<SparseBitVector> colsList;
    /**
     * 矩阵的行向量数
     */
    protected int rows;
    /**
     * 矩阵的列向量数
     */
    protected int cols;
    /**
     * parallel 表示该矩阵对向量的操作是否支持并行。
     */
    protected boolean parallel;

    /**
     * 使用列向量组初始化。
     *
     * @param colsList 列向量组。
     */
    protected void initFromColList(ArrayList<SparseBitVector> colsList) {
        assert colsList.size() > 0 : "colsList must be non-empty";
        this.rows = colsList.get(0).getBitSize();
        for (SparseBitVector col : colsList) {
            assert col.getBitSize() == rows : "all sparse vectors must have the same bit size";
        }
        this.colsList = colsList;
        this.cols = colsList.size();
    }

    /**
     * 作为循环矩阵初始化。
     *
     * @param rows      行数。
     * @param cols      列数。
     * @param initArray 初始列向量。
     */
    protected void initAsCyclicMatrix(int rows, int cols, int[] initArray) {
        this.rows = rows;
        this.cols = cols;
        SparseBitVector initVector = SparseBitVector.create(initArray, rows);
        // 创建colsList
        colsList = new ArrayList<>();
        colsList.ensureCapacity(cols);
        // 循环移位initVector，并写入colsList
        SparseBitVector tempVector = initVector.copyOf();
        for (int j = 0; j < cols; j++) {
            colsList.add(tempVector);
            tempVector = tempVector.cyclicMove();
        }
    }

    /**
     * 稀疏矩阵加法。
     *
     * @param that 另一个稀疏矩阵。
     * @return 加和的列向量组。
     */
    protected ArrayList<SparseBitVector> addToColsList(AbstractSparseBitMatrix that) {
        assert this.rows == that.rows;
        assert this.cols == that.cols;

        IntStream addStream = IntStream.range(0, cols);
        addStream = parallel ? addStream.parallel() : addStream;
        return addStream
            .mapToObj(index -> this.getCol(index).add(that.getCol(index)))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 截取子矩阵。
     *
     * @param startColIndex 开始截取的列位置。
     * @param endColIndex   结束截取的列位置。
     * @param startRowIndex 开始截取的行位置。
     * @param endRowIndex   结束截取的行位置。
     * @return 子矩阵的列向量组。
     */
    protected ArrayList<SparseBitVector> getSubColsList(int startColIndex, int endColIndex, int startRowIndex, int endRowIndex) {
        assert startColIndex >= 0 && endColIndex <= cols && startColIndex <= endColIndex;
        assert startRowIndex >= 0 && endRowIndex <= rows && startRowIndex <= endRowIndex;

        IntStream intStream = IntStream.range(startColIndex, endColIndex);
        return intStream.parallel().
            mapToObj(index -> getCol(index).getSubArray(startRowIndex, endRowIndex)
            ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 获取行向量组。
     *
     * @return 行向量组。
     */
    protected ArrayList<SparseBitVector> getRowsList() {
        ArrayList<int[]> rowsIndexList = new ArrayList<>();
        rowsIndexList.ensureCapacity(rows);
        // init an array to count the number of non-zero points in each row
        int[] targetCounter = new int[rows];
        // count the number column by column
        for (SparseBitVector array : colsList) {
            array.indexCounter(targetCounter);
        }
        // put empty sparse vectors in the rowIndexList
        for (int size : targetCounter) {
            rowsIndexList.add(new int[size]);
        }
        // write non-zero points into the rowsIndexList
        Arrays.fill(targetCounter, 0);
        for (int colIndex = 0; colIndex < cols; colIndex++) {
            for (int rowIndex : getCol(colIndex).getNonZeroIndexArray()) {
                rowsIndexList.get(rowIndex)[targetCounter[rowIndex]++] = colIndex;
            }
        }
        // convert rowsIndexList to rowsList, thus sort
        return rowsIndexList.stream()
            .parallel()
            .map(indexArray -> SparseBitVector.create(indexArray, cols))
            .collect(Collectors.toCollection(ArrayList<SparseBitVector>::new));
    }

    /**
     * 计算布尔向量左乘矩阵 x*M。
     *
     * @param vecX 布尔向量。
     * @return 返回结果。
     */
    public boolean[] lmul(final boolean[] vecX) {
        assert (rows == vecX.length);

        boolean[] outputs = new boolean[cols];
        lmulAddi(vecX, outputs);
        return outputs;
    }

    /**
     * 计算布尔向量左乘矩阵再加向量 x*M +y， 结果返回到y。
     *
     * @param vecX 布尔向量。
     * @param vecY 布尔向量。
     */
    public void lmulAddi(final boolean[] vecX, boolean[] vecY) {
        assert rows == vecX.length;
        assert cols == vecY.length;

        IntStream multiplyIntStream = IntStream.range(0, cols);
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> vecY[i] ^= getCol(i).multiply(vecX));
    }

    /**
     * 计算 byte[][] 左乘矩阵 x*M。
     * byte[]作为扩域GF2E的元素进行乘法运算。
     *
     * @param vecX 向量。
     */
    public byte[][] lExtMul(final byte[][] vecX) {
        assert rows == vecX.length;

        byte[][] outputs = new byte[cols][];
        IntStream multiplyIntStream = IntStream.range(0, getCols());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> outputs[i] = getCol(i).multiply(vecX));
        return outputs;
    }

    /**
     * 计算 byte[][] 左乘矩阵再加向量 x*M +y， 结果返回到y。
     * byte[]作为扩域GF2E的元素进行乘法运算。
     *
     * @param vecX 向量。
     * @param vecY 向量。
     */
    public void lExtMulAddi(final byte[][] vecX, byte[][] vecY) {
        assert rows == vecX.length;
        assert cols == vecY.length;
        assert vecX[0].length == vecY[0].length;

        IntStream multiplyIntStream = IntStream.range(0, getCols());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> getCol(i).multiplyAddi(vecX, vecY[i]));
    }

    /**
     * 计算A^T*M，其中A为当前稀疏矩阵，M为DenseBitMatrix.
     *
     * @param denseBitMatrix DenseBitMatrix M
     * @return A^T*M
     */
    public DenseBitMatrix transMultiply(DenseBitMatrix denseBitMatrix) {
        assert denseBitMatrix.getRows() == rows;

        return (rows == denseBitMatrix.getColumns())
            ? ByteSquareDenseBitMatrix.fromDense(lExtMul(denseBitMatrix.toByteArrays()))
            : ByteDenseBitMatrix.fromDense(denseBitMatrix.getColumns(), lExtMul(denseBitMatrix.toByteArrays()));
    }

    /**
     * 得到转置矩阵的DenseBitMatrix形式。
     *
     * @return 转置矩阵的DenseBitMatrix形式。
     */
    public DenseBitMatrix toTransDenseBitMatrix() {
        IntStream intStream = IntStream.range(0, getCols());
        intStream = parallel ? intStream.parallel() : intStream;
        int[][] positions = intStream.mapToObj(i -> getCol(i).getNonZeroIndexArray()).toArray(int[][]::new);
        return (rows == cols)
            ? ByteSquareDenseBitMatrix.fromSparse(positions)
            : ByteDenseBitMatrix.fromSparse(rows, positions);
    }

    /**
     * 得到极度稀疏矩阵形式。
     *
     * @return 极度稀疏矩阵形式。
     */
    public ExtremeSparseBitMatrix toExtremeSparseMatrix() {
        ArrayList<Integer> indexList = new ArrayList<>();
        ArrayList<SparseBitVector> nColsList = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            if (getCol(i).getSize() != 0) {
                nColsList.add(getCol(i));
                indexList.add(i);
            }
        }
        int[] nonEmptyIndex = indexList.stream().mapToInt(k -> k).toArray();
        return new ExtremeSparseBitMatrix(nColsList, nonEmptyIndex, rows, cols);
    }


    /**
     * 读取稀疏矩阵的行数
     *
     * @return 返回稀疏矩阵的行数
     */
    public int getRows() {
        return rows;
    }

    /**
     * 读取稀疏矩阵的列数。
     *
     * @return 返回稀疏矩阵的列数。
     */
    public int getCols() {
        return cols;
    }

    /**
     * 返回指定列。
     *
     * @param j 指定的列数。
     * @return 对应的列。
     */
    public SparseBitVector getCol(int j) {
        return colsList.get(j);
    }

    /**
     * 设置是否并行。
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractSparseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        AbstractSparseBitMatrix that = (AbstractSparseBitMatrix) obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder().append(this.colsList, that.colsList);
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(colsList).toHashCode();
    }

}
