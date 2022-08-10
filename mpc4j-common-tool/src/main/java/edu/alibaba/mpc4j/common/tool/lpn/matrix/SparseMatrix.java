package edu.alibaba.mpc4j.common.tool.lpn.matrix;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 稀疏矩阵相关代数操作类，用于支持LDPC 生成、存储和编码
 * 稀疏矩阵是指 只有少数点为1，多数点为0的 比特矩阵。
 * 代码部分参考libOTe/Tools/LDPC/Mtx.cpp 和 Mtx.h
 *
 * @author Hanwen Feng
 * @date 2021.12.17
 */

public class SparseMatrix {
    /**
     * 按行存储稀疏矩阵中值为1的点的位置
     * SparseArray 是SparseMtx内部类，封装了 int[]
     * 例如，若rowsList.get(0) = [1,4,7]，表示矩阵第0行的第1、4、7个元素为1,该行其余元素为0。
     */
    private ArrayList<SparseVector> rowsList;
    /**
     * 按列存储稀疏矩阵中值为1的点的位置
     * 例如，若colsList.get(0) = [1,4,7]，表示矩阵第0列的第1、4、7个元素为1,该列其余元素为0。
     */
    private ArrayList<SparseVector> colsList;
    /**
     * 矩阵的行向量数
     */
    private int rows;
    /**
     * 矩阵的列向量数
     */
    private int cols;
    /**
     * diagMtx 为 true，表示该矩阵为下三角阵，可以支持专属的求逆运算。
     */
    private boolean diagMtx;
    /**
     * parallel 表示该矩阵对向量的操作是否支持并行。
     */
    private boolean parallel;

    /**
     * WorkType 定义了该稀疏矩阵的存储和运算模式
     * ColsOnly： 该矩阵仅按列存储位置向量，即仅使用 colsList，也仅支持按列执行的操作
     * RowsOnly:  该矩阵仅按行存储位置向量，即仅使用 rowsList，也仅支持按行执行的操作
     * Full: 同时存储和使用 colsList 和 rowsList
     */
    public enum WorkType {
        ColsOnly,
        RowsOnly,
        Full,
    }

    /**
     * 默认WorkType 为 Full
     */
    private WorkType type = WorkType.Full;


    /*
     *****************************************************
     * SparseMtx类对象创建方法 开始线
     * ***************************************************
     */

    /**
     * 私有构造函数
     */
    private SparseMatrix() {
        //empty
    }

    /**
     * 根据指定的rowsList和colsList构造稀疏矩阵
     *
     * @param rowsList 按行存储的list
     * @param colsList 按列存储的list
     */
    public SparseMatrix(ArrayList<SparseVector> rowsList, ArrayList<SparseVector> colsList) {
        this.colsList = colsList;
        this.rowsList = rowsList;
        cols = colsList.size();
        rows = rowsList.size();
        assert isValid();
    }

    /**
     * 根据指定的list和type构造稀疏矩阵
     *
     * @param type 说明按行存储或者按列存储
     * @param list 按列或者按列存储的list
     */
    public SparseMatrix(ArrayList<SparseVector> list, WorkType type, int rows, int cols) {
        assert type != WorkType.Full;
        this.type = type;
        switch (type) {
            case RowsOnly: {
                this.rowsList = list;
                break;
            }
            case ColsOnly: {
                this.colsList = list;
                break;
            }
            default:
                throw new IllegalArgumentException("Do not accept FULL type");
        }
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * 根据指定的List<PointList.Point>创建稀疏矩阵。根据指定type，按照行list和或按照列的list存储。
     * 例如mRowsList.get(r).getValue(rIndex)=c,表示该矩阵第r行第c列为1
     * 此时，一定存在cIndex,使得 mColsList.get(c).getValue(cIndex)=r
     * 要求每一行、每一列内按照升序排列
     *
     * @param rows   指定的行数
     * @param cols   指定的列数
     * @param points 指定的点列
     * @param type   所创建矩阵的存储和工作模式
     */
    public static SparseMatrix createFromPoints(int rows, int cols, List<Point> points, WorkType type) {
        SparseMatrix mtx = new SparseMatrix();
        mtx.type = type;
        mtx.rows = rows;
        mtx.cols = cols;
        boolean workAsRows = type == WorkType.Full || type == WorkType.RowsOnly;
        boolean workAsCols = type == WorkType.Full || type == WorkType.ColsOnly;
        int[] rowSizes = new int[rows];
        mtx.rowsList = new ArrayList<>();
        int[] colSizes = new int[cols];
        mtx.colsList = new ArrayList<>();

        for (Point p : points) {
            // 检查点是否重复，点的坐标是否在指定范围内。
            assert (p.colIndex < cols);
            assert (p.rowIndex < rows);
            // 对应将该点所处的行、列的元素个数加1。
            if (workAsRows) {
                rowSizes[p.rowIndex]++;
            }
            if (workAsCols) {
                colSizes[p.colIndex]++;
            }
        }

        // 按照得到的每行每列的非零元素个数，初始化各行各列。
        if (workAsRows) {
            for (int i = 0; i < rows; i++) {
                SparseVector row = new SparseVector(rowSizes[i], cols);
                mtx.rowsList.add(row);
                // 置为零，留作后续写入各点时计数使用。
                rowSizes[i] = 0;
            }
        }
        if (workAsCols) {
            for (int i = 0; i < cols; i++) {
                SparseVector col = new SparseVector(colSizes[i], rows);
                mtx.colsList.add(col);
                // 置为零，留作后续写入各点时计数使用。
                colSizes[i] = 0;
            }
        }
        // 重新读取各点，并将相应数据记录入rowsList和colsList中。
        for (Point p : points) {
            // 读取点p的行坐标r和列坐标c。
            int r = p.rowIndex;
            int c = p.colIndex;
            if (workAsRows) {
                // 将第r行的元素个数+1，得到点p是第r行的第prindex个元素。
                int pRIndex = rowSizes[r]++;
                // 在第r行的第pRIndex个元素记录该点的列坐标c。
                mtx.rowsList.get(r).setValue(pRIndex, c);
            }
            if (workAsCols) {
                // 逐列进行上述操作。
                int pCIndex = colSizes[c]++;
                mtx.colsList.get(c).setValue(pCIndex, r);
            }
        }
        // 将各行各列按照升序进行排序。
        if (workAsRows) {
            for (int i = 0; i < rows; i++) {
                mtx.rowsList.get(i).sort();
            }
        }
        if (workAsCols) {
            for (int i = 0; i < cols; i++) {
                mtx.colsList.get(i).sort();
            }
        }
        return mtx;
    }

    /**
     * 直接使用PointList类型作为输入
     */
    public static SparseMatrix createFromPointsList(PointList pointList, WorkType type) {
        return SparseMatrix.createFromPoints(pointList.maxRowIndex, pointList.maxColIndex, pointList.pointsArrayList, type);
    }


    /**
     * 从一个稀疏行向量循环生成一个稀疏矩阵
     *
     * @param rows    指定行数
     * @param cols    指定列数
     * @param initVec 首个稀疏行向量
     * @param type    生成稀疏矩阵的存储和工作模式
     * @return 返回生成的稀疏矩阵
     */
    public static SparseMatrix createFromCyclicRow(int rows, int cols, int[] initVec, WorkType type) {
        assert (type == WorkType.RowsOnly || type == WorkType.Full);
        SparseMatrix mtx = new SparseMatrix();
        mtx.rows = rows;
        mtx.cols = cols;
        mtx.initFromCyclicArray(rows, cols, initVec, type, true);
        return mtx;
    }

    /**
     * 从一个稀疏列向量循环生成一个稀疏矩阵
     * 同上
     */
    public static SparseMatrix createFromCyclicCol(int rows, int cols, int[] initVec, WorkType type) {
        assert (type == WorkType.ColsOnly || type == WorkType.Full);
        SparseMatrix mtx = new SparseMatrix();
        mtx.rows = rows;
        mtx.cols = cols;
        mtx.initFromCyclicArray(rows, cols, initVec, type, false);
        return mtx;
    }

    /**
     * 将一个稀疏矩阵初始化为循环矩阵
     *
     * @param rows            指定行数
     * @param cols            指定列数
     * @param initVec         初始向量
     * @param type            矩阵的存储和工作模式
     * @param isInitVectorRow 判断稀疏向量是行向量还是列向量
     */
    private void initFromCyclicArray(int rows, int cols, int[] initVec, WorkType type, boolean isInitVectorRow) {
        this.type = type;

        assert ((isInitVectorRow && type != WorkType.ColsOnly) ||
                (!isInitVectorRow && type != WorkType.RowsOnly));

        if (isInitVectorRow) {
            rowsList = new ArrayList<>();
            SparseVector initArray = new SparseVector(initVec, cols);
            if (!initArray.isSorted()) {
                initArray.sort();
            }
            generateCyclicList(initArray, rows, rowsList);
        } else {
            colsList = new ArrayList<>();
            SparseVector initArray = new SparseVector(initVec, rows);
            if (!initArray.isSorted()) {
                initArray.sort();
            }
            generateCyclicList(initArray, cols, colsList);
        }

        if (type == WorkType.Full) {
            if (isInitVectorRow) {
                colsList = completeMtx(rowsList);
            } else {
                rowsList = completeMtx(colsList);
            }
        }
    }

    /**
     * 根据DenseMtx 生成稀疏矩阵
     *
     * @param dMtx 常规的比特矩阵
     */

    public static SparseMatrix createFromDenseMtx(DenseMatrix dMtx, WorkType type) {
        int rows = dMtx.getRows();
        int cols = dMtx.getCols();
        PointList pnts = new PointList(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (dMtx.getValue(i, j))
                    pnts.addPoint(new Point(i, j));
            }
        }
        return SparseMatrix.createFromPointsList(pnts, type);
    }



    /*
     *****************************************************
     * SparseMtx类对象创建方法*****END******
     * ***************************************************
     */

    /*
     *****************************************************
     * SparseMtx类代数运算方法*****BEGIN******
     * ***************************************************
     */

    /**
     * 计算布尔向量左乘矩阵 x*M
     *
     * @param vecX 布尔向量
     * @return 返回结果
     */
    public boolean[] lmul(final boolean[] vecX) {
        assert (type != WorkType.RowsOnly);
        assert (getRows() == vecX.length);

        boolean[] outputs = new boolean[getCols()];
        IntStream multiplyIntStream = IntStream.range(0, getCols());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> outputs[i] = getCol(i).multiply(vecX));
        return outputs;
    }

    /**
     * 计算布尔向量左乘矩阵再加向量 x*M +y， 结果返回到y
     *
     * @param vecX 布尔向量
     * @param vecY 布尔向量
     */
    public void lmulAdd(final boolean[] vecX, boolean[] vecY) {
        assert (type != WorkType.RowsOnly);
        assert (getRows() == vecX.length);

        IntStream multiplyIntStream = IntStream.range(0, getCols());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> vecY[i] ^= getCol(i).multiply(vecX));
    }

    /**
     * 计算布尔向量右乘矩阵 M*x
     *
     * @param vecX 布尔向量
     * @return 返回结果
     */
    public boolean[] rmul(final boolean[] vecX) {
        assert (type != WorkType.ColsOnly);
        assert (getCols() == vecX.length);

        boolean[] outputs = new boolean[getRows()];
        IntStream multiplyIntStream = IntStream.range(0, getRows());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> outputs[i] = getRow(i).multiply(vecX));
        return outputs;
    }

    /**
     * 计算布尔向量右乘矩阵再加向量 M *x+y， 结果返回到y
     *
     * @param vecX 布尔向量
     * @param vecY 布尔向量
     */
    public void rmulAdd(final boolean[] vecX, boolean[] vecY) {
        assert (type != WorkType.RowsOnly);
        assert (getRows() == vecX.length);

        IntStream multiplyIntStream = IntStream.range(0, getRows());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> vecY[i] ^= getRow(i).multiply(vecX));
    }


    /**
     * 计算 byte[][] 左乘 矩阵 x*M. byte[][] 可表示一般的 F_p^n, 其中一个F_p的元素表示为byte[]
     *
     * @param vecX 向量x
     * @return 返回乘积
     */
    public byte[][] lmul(final byte[][] vecX) {
        assert (type != WorkType.RowsOnly);
        assert (getRows() == vecX.length);

        byte[][] outputs = new byte[getCols()][];
        IntStream multiplyIntStream = IntStream.range(0, getCols());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> outputs[i] = getCol(i).multiply(vecX));
        return outputs;
    }

    /**
     * 计算 byte[][] 左乘矩阵再加向量 x*M +y， 结果返回到y
     *
     * @param vecX 向量
     * @param vecY 向量
     */
    public void lmulAdd(final byte[][] vecX, byte[][] vecY) {
        assert (type != WorkType.RowsOnly);
        assert (getRows() == vecX.length);

        IntStream multiplyIntStream = IntStream.range(0, getCols());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> getCol(i).multiplyAdd(vecX, vecY[i]));
    }

    /**
     * 计算 byte[][] 右乘 矩阵 M*x. byte[][] 可表示一般的 F_p^n, 其中一个F_p的元素表示为byte[]
     *
     * @param vecX 向量x
     * @return 返回乘积
     */
    public byte[][] rmul(final byte[][] vecX) {
        assert (type != WorkType.ColsOnly);
        assert (getCols() == vecX.length);

        byte[][] outputs = new byte[getRows()][];
        IntStream multiplyIntStream = IntStream.range(0, getRows());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> outputs[i] = getRow(i).multiply(vecX));
        return outputs;
    }

    /**
     * 计算byte[][]右乘矩阵再加向量 M *x+y， 结果返回到y
     *
     * @param vecX 向量
     * @param vecY 向量
     */
    public void rmulAdd(final byte[][] vecX, byte[][] vecY) {
        assert (type != WorkType.RowsOnly);
        assert (getRows() == vecX.length);

        IntStream multiplyIntStream = IntStream.range(0, getRows());
        multiplyIntStream = parallel ? multiplyIntStream.parallel() : multiplyIntStream;
        multiplyIntStream.forEach(i -> getRow(i).multiplyAdd(vecX, vecY[i]));
    }

    /**
     * 适用于下三角矩阵C且对角线全为1. 计算 C^{-1}*e =x. 等价于求解方程组 Cx = e
     *
     * @param vecE 输入向量e
     * @return 输出向量 x
     */
    public boolean[] diagInvRmul(final boolean[] vecE) {
        assert isDiagMtx();
        assert type != WorkType.ColsOnly;
        assert vecE.length == getRows();

        boolean[] outputs = new boolean[getCols()];

        for (int i = 0; i < getRows(); i++) {
            outputs[i] = vecE[i];
            for (int j = 0; j < getRow(i).getSize() - 1; j++) {
                int index = getRow(i).getValue(j);
                outputs[i] ^= outputs[index];
            }
        }
        return outputs;
    }

    /**
     * 适用于下三角矩阵C且对角线全为1. 计算 C^{-1}*e +y. 将结果返回到y
     *
     * @param vecE 输入向量e
     * @param vecY 向量y
     */
    public void diagInvRmulAdd(final boolean[] vecE, boolean[] vecY) {
        assert vecY.length == getCols();
        boolean[] outputs = diagInvRmul(vecE);
        for (int i = 0; i < vecY.length; i++) {
            vecY[i] ^= outputs[i];
        }
    }

    /**
     * 适用于下三角矩阵C且对角线全为1. 计算 e*C^{-1} =x. 等价于求解方程组 xC = e （或者 C^T*x =e）
     *
     * @param vecE 输入向量e
     * @return 输出向量 x
     */
    public boolean[] diagInvLmul(final boolean[] vecE) {
        assert isDiagMtx();
        assert type != WorkType.RowsOnly;
        assert vecE.length == getCols();

        boolean[] outputs = new boolean[getRows()];

        for (int i = getCols() - 1; i >= 0; --i) {
            outputs[i] = vecE[i];
            for (int j = 1; j < getCol(i).getSize(); j++) {
                int index = getCol(i).getValue(j);
                outputs[i] ^= outputs[index];
            }
        }
        return outputs;
    }

    /**
     * 适用于下三角矩阵C且对角线全为1. 计算 e*C^{-1} +y. 将结果返回到y
     *
     * @param vecE 输入向量e
     * @param vecY 向量y
     */
    public void diagInvLmulAdd(final boolean[] vecE, boolean[] vecY) {
        assert vecY.length == getRows();
        boolean[] outputs = diagInvLmul(vecE);
        for (int i = 0; i < vecY.length; i++) {
            vecY[i] ^= outputs[i];
        }
    }

    /**
     * 适用于下三角矩阵C且对角线全为1. 计算 C^{-1}*e =x. 等价于求解方程组 Cx = e.
     * byte[][] 可表示一般的 F_p^n, 其中一个F_p的元素表示为byte[]
     *
     * @param vecE 输入向量e
     * @return 输出向量 x
     */
    public byte[][] diagInvRmul(final byte[][] vecE) {
        assert isDiagMtx();
        assert type != WorkType.ColsOnly;
        assert vecE.length == getRows();

        byte[][] outputs = new byte[getCols()][];

        for (int i = 0; i < getRows(); i++) {
            outputs[i] = BytesUtils.clone(vecE[i]);
            for (int j = 0; j < getRow(i).getSize() - 1; j++) {
                int index = getRow(i).getValue(j);
                BytesUtils.xori(outputs[i], outputs[index]);
            }
        }
        return outputs;
    }

    /**
     * 适用于下三角矩阵C且对角线全为1. 计算 C^{-1}*e +y. 将结果返回到y
     *
     * @param vecE 输入向量e
     * @param vecY 向量y
     */
    public void diagInvRmulAdd(final byte[][] vecE, byte[][] vecY) {
        assert vecY.length == getCols();
        byte[][] outputs = diagInvRmul(vecE);
        for (int i = 0; i < vecY.length; i++) {
            BytesUtils.xori(vecY[i], outputs[i]);
        }
    }

    /**
     * 适用于下三角矩阵C且对角线全为1. 计算 e*C^{-1} =x. 等价于求解方程组 xC = e （或者 C^T*x =e）
     *
     * @param vecE 输入向量e
     * @return 输出向量 x
     */
    public byte[][] diagInvLmul(final byte[][] vecE) {
        assert isDiagMtx();
        assert type != WorkType.RowsOnly;
        assert vecE.length == getCols();

        byte[][] outputs = new byte[getRows()][];

        for (int i = getCols() - 1; i >= 0; --i) {
            outputs[i] = BytesUtils.clone(vecE[i]);
            for (int j = 1; j < getCol(i).getSize(); j++) {
                int index = getCol(i).getValue(j);
                BytesUtils.xori(outputs[i], outputs[index]);
            }
        }
        return outputs;
    }

    /**
     * 适用于下三角矩阵C且对角线全为1. 计算 e*C^{-1} +y. 将结果返回到y
     *
     * @param vecE 输入向量e
     * @param vecY 向量y
     */
    public void diagInvLmulAdd(final byte[][] vecE, byte[][] vecY) {
        assert vecY.length == getRows();
        byte[][] outputs = diagInvLmul(vecE);
        for (int i = 0; i < vecY.length; i++) {
            BytesUtils.xori(vecY[i], outputs[i]);
        }
    }

    /**
     * 两个稀疏矩阵相乘
     *
     * @param sparseMatrix 另一个稀疏矩阵
     * @return 返回稀疏矩阵
     */
    public SparseMatrix multi(SparseMatrix sparseMatrix, WorkType targetType) {
        assert (getCols() == sparseMatrix.getRows());
        assert type != WorkType.ColsOnly;
        assert sparseMatrix.type != WorkType.RowsOnly;

        ArrayList<Point> points = new ArrayList<>();
        // 按照一行乘一列进行。
        for (int i = 0; i < getRows(); ++i) {
            // 读取本矩阵的行。
            SparseVector rowI = getRow(i);
            for (int j = 0; j < sparseMatrix.getCols(); ++j) {
                // 读取另一个矩阵的列。
                SparseVector colJ = sparseMatrix.getCol(j);
                // 将行与列做内积。
                boolean result = rowI.multiply(colJ);
                // 若内积为真，则点(i,j)在乘积矩阵中值为1,加入点积。
                if (result) {
                    points.add(new Point(i, j));
                }
            }
        }
        return SparseMatrix.createFromPoints(getRows(), sparseMatrix.getCols(), points, targetType);
    }

    /**
     * 两个稀疏矩阵相加
     *
     * @param sparseMatrix 另一个稀疏矩阵
     * @return 返回稀疏矩阵
     */
    public SparseMatrix add(SparseMatrix sparseMatrix, WorkType targetType) {
        assert (getRows() == sparseMatrix.getRows());
        assert (getCols() == sparseMatrix.getCols());
        assert (type == sparseMatrix.type);
        assert type == WorkType.Full || targetType == type;

        SparseMatrix rMtx = new SparseMatrix();
        rMtx.rows = getRows();
        rMtx.cols = getCols();
        rMtx.type = targetType;

        switch (targetType) {
            case RowsOnly: {
                ArrayList<SparseVector> rRowsList = new ArrayList<>(getRows());
                sparseListAddI(rowsList, sparseMatrix.rowsList, rRowsList);
                rMtx.rowsList = rRowsList;
                break;
            }
            case ColsOnly: {
                ArrayList<SparseVector> rColsList = new ArrayList<>(getCols());
                sparseListAddI(colsList, sparseMatrix.colsList, rColsList);
                rMtx.colsList = rColsList;
                break;
            }
            case Full: {
                ArrayList<SparseVector> rRowsList = new ArrayList<>(getRows());
                sparseListAddI(rowsList, sparseMatrix.rowsList, rRowsList);
                rMtx.colsList = completeMtx(rRowsList);
                rMtx.rowsList = rRowsList;
            }
        }
        return rMtx;
    }

    /**
     * 对于下三角方针C和密集矩阵Y，计算 Y*C^-1 = R。 等价于求解 RC= Y
     *
     * @param denseMatrix 输入的密集矩阵Y
     * @return 计算结果R
     */
    public DenseMatrix diagInvLmul(DenseMatrix denseMatrix) {
        byte[][] pureDenseMtx = diagInvLmul(denseMatrix.getPureMatrix());
        return new DenseMatrix(denseMatrix.getRows(), getRows(), pureDenseMtx);
    }

    /**
     * 对于下三角方针C和密集矩阵Y，计算 C^-1*Y = R。 等价于求解 CR= Y
     *
     * @param denseMatrix 输入的密集矩阵Y
     * @return 计算结果R
     */
    public DenseMatrix diagInvRmul(DenseMatrix denseMatrix) {
        byte[][] pureDenseMtx = diagInvRmul(denseMatrix.getPureMatrix());
        return new DenseMatrix(pureDenseMtx.length, pureDenseMtx[0].length, pureDenseMtx);
    }

    /**
     * 对于稀疏矩阵M和密集矩阵Y，计算 M*Y = R。
     *
     * @param denseMatrix 输入的密集矩阵Y
     * @return 计算结果R
     */
    public DenseMatrix rmul(DenseMatrix denseMatrix) {
        byte[][] pureDenseMtx = rmul(denseMatrix.getPureMatrix());
        return new DenseMatrix(getRows(), denseMatrix.getCols(), pureDenseMtx);
    }

    /**
     * 对于稀疏矩阵M和密集矩阵Y，计算 Y*M = R。
     *
     * @param denseMatrix 输入的密集矩阵Y
     * @return 计算结果R
     */
    public DenseMatrix lmul(DenseMatrix denseMatrix) {
        byte[][] pureDenseMtx = lmul(denseMatrix.getPureMatrix());
        return new DenseMatrix(denseMatrix.getRows(), getCols(), pureDenseMtx);
    }

    /**
     * 对于下三角的稀疏矩阵C,计算稀疏矩阵X=C^-1Y，等价于求解C*X=Y。
     * 由于X大概率不稀疏，该运算效率实测较低。
     *
     * @param yMtx 稀疏矩阵Y
     * @return 稀疏矩阵X
     */
    public SparseMatrix diagInvertRightMulti(final SparseMatrix yMtx) {
        assert isDiagMtx();
        int xNumberOfRows = getCols();
        int xNumberOfCols = yMtx.getCols();
        ArrayList<SparseVector> xColsList = new ArrayList<>(xNumberOfCols);

        // 逐列计算矩阵X。
        for (int c = 0; c < yMtx.getCols(); c++) {
            SparseVector yColC = yMtx.getCol(c);
            int yIndex = 0;
            ArrayList<Integer> temList4xCol = new ArrayList<>();
            for (int i = 0; i < xNumberOfRows; i++) {
                // 决定X的该列是否包含第i行的比特。
                byte bit = 0;
                // 如果y该列的第i行为1，由于C是对角阵，则首先将该bit置为1。
                if (yIndex != yColC.getSize() && yColC.getValue(yIndex) == i) {
                    bit = 1;
                    ++yIndex;
                }
                // 读取矩阵C第i行。由于最后一个1必然出现在对角线上，上述判断已经考虑，因此在msize-1处结束。
                SparseVector rowI = getRow(i);
                int index = 0;
                int end = rowI.getSize() - 1;
                int indexOfTem = 0;
                while (index != end && indexOfTem != temList4xCol.size()) {
                    if (rowI.getValue(index) < temList4xCol.get(indexOfTem))
                        ++index;
                    else if (temList4xCol.get(indexOfTem) < rowI.getValue(index))
                        ++indexOfTem;
                    else {
                        bit ^= 1;
                        ++index;
                        ++indexOfTem;
                    }
                }
                if (bit == 1) {
                    temList4xCol.add(i);
                }
            }
            // list 返回的数组修改不会影响原list。
            xColsList.add(new SparseVector(temList4xCol.stream().mapToInt(Integer::intValue).toArray(), getRows()));
        }
        ArrayList<SparseVector> xRowsList = completeMtx(xColsList);
        return new SparseMatrix(xRowsList, xColsList);
    }

    /*
     *****************************************************
     * SparseMtx类代数运算方法*****END******
     * ***************************************************
     */

    /**
     * 检查当前的稀疏矩阵格式是否正确
     */
    public boolean isValid() {
        // 记录每一列读到的位置。
        int[] colIterators = new int[getCols()];
        for (int i = 0; i < getRows(); ++i) {
            // 判断每一行是否都已经排序。
            SparseVector rowI = rowsList.get(i);
            if (!rowI.isSorted()) {
                System.out.println("unsorted");
                return false;
            }

            for (int j = 0; j < rowI.getSize(); j++) {
                // 判断行内是否有超过总列数的编号。
                if (rowI.getValue(j) >= getCols()) {
                    System.out.println("Out Of ColNumber");
                    return false;
                }
                // 判断该位置对应的列编号是否已经走完。
                if (colIterators[rowI.getValue(j)] >= colsList.get(rowI.getValue(j)).getSize()) {
                    System.out.println("The row index is :" + i);
                    System.out.println("the index in the row is: " + j);
                    System.out.println("the value is : " + rowI.getValue(j));
                    System.out.println("col is end");
                    return false;
                }
                // 判断该位置对应的列向量位置是否存储为i行。如果满足这个判断，则列向量也一定排列好了。
                int colIndexNow = colsList.get(rowI.getValue(j)).getValue(colIterators[rowI.getValue(j)]);
                if (colIndexNow != i) {
                    System.out.println(" test col is unsorted");
                    if (colsList.get(rowI.getValue(j)).isSorted())
                        System.out.println("but it is already sorted");
                    return false;
                }
                colIterators[rowI.getValue(j)]++;
            }
        }
        return true;
    }


    /**
     * 将两个稀疏矩阵纵向拼接为一个新稀疏矩阵，本来的稀疏矩阵在上边
     *
     * @param sMtx 需要拼接的另一个稀疏矩阵
     * @return 返回新的稀疏矩阵
     */
    public SparseMatrix vConcat(SparseMatrix sMtx) {
        // 首先判断两个矩阵的列数是否相同。
        assert (getCols() == sMtx.getCols());
        // 将两个矩阵均转换为pointList。
        PointList mPointList = convert2PointList();
        PointList newPointList = sMtx.convert2PointList();
        // 新矩阵的行数为两者之和。
        mPointList.maxRowIndex += newPointList.maxRowIndex;
        // 将待添加的列的每个点行坐标后移，添加。
        for (Point point : newPointList.pointsArrayList) {
            mPointList.addPoint(new Point(point.rowIndex + getRows(), point.colIndex));
        }
        // 返回新矩阵。
        return SparseMatrix.createFromPoints(mPointList.maxRowIndex, mPointList.maxColIndex, mPointList.pointsArrayList, WorkType.Full);
    }

    /**
     * 检查在该稀疏矩阵中，指定坐标的点是否记录，即该点是否为1。
     *
     * @param rowIndex 指定的行坐标
     * @param colIndex 指定的列坐标
     * @return 返回判断值
     */
    public boolean isOne(int rowIndex, int colIndex) {
        // 首先判断输入是否在范围内。
        assert (rowIndex < getRows());
        assert (colIndex < getCols());

        return getRow(rowIndex).checkIndex(colIndex);
    }

    /**
     * 根据指定的起始位置和行列大小，从稀疏矩阵中提取子矩阵
     *
     * @param startRowIndex  子矩阵开始的行坐标
     * @param startColIndex  子矩阵开始的列坐标
     * @param selectRowCount 子矩阵的行数
     * @param selectColCount 子矩阵的列数
     * @return 提取的子矩阵
     */
    public SparseMatrix getSubMatrix(final int startRowIndex, final int startColIndex, final int selectRowCount,
                                     final int selectColCount, WorkType type) {
        assert (type == this.type || this.type == WorkType.Full);

        assert (selectColCount > 0 && selectRowCount > 0);
        int endRowIndex = startRowIndex + selectRowCount;
        int endColIndex = startColIndex + selectColCount;
        assert (this.getRows() >= endRowIndex);
        assert (this.getCols() >= endColIndex);

        switch (type) {
            case RowsOnly: {
                ArrayList<SparseVector> nRowsList = getSubSparseList(this.rowsList, startRowIndex, endRowIndex, startColIndex, endColIndex);
                return new SparseMatrix(nRowsList, type, selectRowCount, selectColCount);
            }
            case ColsOnly: {
                ArrayList<SparseVector> nColsList = getSubSparseList(this.colsList, startColIndex, endColIndex, startRowIndex, endRowIndex);
                return new SparseMatrix(nColsList, type, selectRowCount, selectColCount);
            }
            case Full: {
                ArrayList<SparseVector> nRowsList = getSubSparseList(this.rowsList, startRowIndex, endRowIndex, startColIndex, endColIndex);
                ArrayList<SparseVector> nColsList = getSubSparseList(this.colsList, startColIndex, endColIndex, startRowIndex, endRowIndex);
                return new SparseMatrix(nRowsList, nColsList);
            }
            default:
                throw new IllegalArgumentException("Unknown Exception");
        }
    }

    /**
     * 得到本稀疏矩阵的DenseMatrix形式. 仅支持类型为非ColsOnly的矩阵。
     *
     * @return DenseMatrix
     */
    public DenseMatrix getDense() {
        assert type != WorkType.ColsOnly;
        IntStream intStream = IntStream.range(0, getRows());
        intStream = parallel ? intStream.parallel() : intStream;
        byte[][] pureDenseMtx = intStream.mapToObj(i -> getRow(i).getBitVector()).toArray(byte[][]::new);
        return new DenseMatrix(getRows(), getCols(), pureDenseMtx);
    }

    /**
     * 得到本稀疏矩阵的转置后的DenseMatrix，不支持RowsOnly
     *
     * @return DenseMatrix
     */
    public DenseMatrix getTransDense() {
        assert type != WorkType.RowsOnly;
        IntStream intStream = IntStream.range(0, getCols());
        intStream = parallel ? intStream.parallel() : intStream;
        byte[][] pureDenseMtx = intStream.mapToObj(i -> getCol(i).getBitVector()).toArray(byte[][]::new);
        return new DenseMatrix(getCols(), getRows(), pureDenseMtx);
    }

    /**
     * 得到本稀疏矩阵的极度稀疏 ExtremeSparseMatrix 表示，
     *
     * @return 极度稀疏矩阵 ExtremeSparseMatrix
     */
    public ExtremeSparseMatrix getExtremeSparseMatrix() {
        assert type != WorkType.RowsOnly;

        ArrayList<Integer> indexList = new ArrayList<>();
        ArrayList<SparseVector> nColsList = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            if (getCol(i).getSize() != 0) {
                nColsList.add(getCol(i));
                indexList.add(i);
            }
        }
        int[] nonEmptyIndex = indexList.stream().mapToInt(k -> k).toArray();
        return new ExtremeSparseMatrix(nColsList, nonEmptyIndex, rows, cols);
    }

    /**
     * 切换本稀疏矩阵的存储类型。根据指定类型，补全缺失存储或者删除多余存储
     *
     * @param type 指定的新存储类型
     */
    public void setType(WorkType type) {
        switch (this.type) {
            case Full: {
                switch (type) {
                    case Full:
                        break;
                    case ColsOnly: {
                        rowsList = null;
                        break;
                    }
                    case RowsOnly: {
                        colsList = null;
                        break;
                    }
                }
                break;
            }
            case RowsOnly: {
                switch (type) {
                    case Full: {
                        colsList = completeMtx(rowsList);
                        break;
                    }
                    case ColsOnly: {
                        colsList = completeMtx(rowsList);
                        rowsList = null;
                        break;
                    }
                    case RowsOnly:
                        break;
                }
                break;
            }
            case ColsOnly: {
                switch (type) {
                    case Full: {
                        rowsList = completeMtx(colsList);
                        break;
                    }
                    case ColsOnly: {
                        break;
                    }
                    case RowsOnly: {
                        rowsList = completeMtx(colsList);
                        colsList = null;
                        break;
                    }
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown Type");
        }
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
     * 读取稀疏矩阵的列
     *
     * @return 返回稀疏矩阵的列数
     */
    public int getCols() {
        return cols;
    }

    /**
     * 返回指定行
     *
     * @param i 指定的行数
     * @return 对应的行
     */
    public SparseVector getRow(int i) {
        return rowsList.get(i);
    }

    /**
     * 返回指定列
     *
     * @param j 指定的列数
     * @return 对应的列
     */
    public SparseVector getCol(int j) {
        return colsList.get(j);
    }

    /**
     * 将稀疏矩阵设置为下三角矩阵，以支持下三角矩阵专属运算。
     */
    public void setDiagMtx() {
        try {
            assert (isDiagMtx());
            diagMtx = true;
        } catch (AssertionError e) {
            throw new IllegalStateException("This is not a diag matrix!");
        }
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * 判断本稀疏矩阵是否为下三角矩阵
     *
     * @return 返回判断结果
     */
    public boolean isDiagMtx() {
        if (diagMtx) {
            return true;
        }
        if (rows != cols) {
            return false;
        }
        if (type == WorkType.RowsOnly || type == WorkType.Full) {
            for (int i = 0; i < getRows(); i++) {
                SparseVector row = getRow(i);
                if (row.getValue(row.getSize() - 1) != i) {
                    return false;
                }
            }
        } else {
            for (int j = 0; j < getCols(); j++) {
                SparseVector col = getCol(j);
                if (col.getValue(0) != j) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     *****************************************************
     * SparseMtx类内部static方法*****BEGIN******
     * ***************************************************
     */


    /**
     * 根据一个稀疏向量，多次移位，将所有移位得到的向量存入list中
     *
     * @param initArray 初始的稀疏向量
     * @param listSize  移位的次数
     * @param list      存入的list
     */
    private static void generateCyclicList(SparseVector initArray, int listSize, List<SparseVector> list) {
        assert listSize >= 0;
        SparseVector tempArray = initArray.copyOf();
        for (int j = 0; j < listSize; j++) {
            list.add(tempArray);
            tempArray = tempArray.cyclicMove();
        }
    }

    /**
     * 根据按行（或者按列）存储的list，计算出该矩阵 按列 （或者按行）存储的list
     *
     * @param originList 已知的list，按行或者按列存储
     * @return 重新存储的list
     */
    private static ArrayList<SparseVector> completeMtx(List<SparseVector> originList) {
        ArrayList<SparseVector> targetList = new ArrayList<>();

        int[] targetCounter = new int[originList.get(0).getBitSize()];

        for (SparseVector array : originList) {
            array.indexCounter(targetCounter);
        }

        for (int size : targetCounter) {
            targetList.add(new SparseVector(size, originList.size()));
        }

        Arrays.fill(targetCounter, 0);
        for (int i = 0; i < originList.size(); i++) {
            for (int j = 0; j < originList.get(i).getSize(); j++) {
                int index = originList.get(i).getValue(j);
                targetList.get(index).setValue(targetCounter[index]++, i);
            }
        }

        for (SparseVector array : targetList) {
            array.setSortedWithCheck();
        }

        return targetList;
    }

    /**
     * 将稀疏矩阵转换为点列
     *
     * @return PointList
     */
    private PointList convert2PointList() {
        return new PointList(getRows(), getCols(), convert2ArrayList());
    }

    /**
     * 将稀疏矩阵转换为点的ArrayList
     *
     * @return ArrayList
     */
    private ArrayList<Point> convert2ArrayList() {
        ArrayList<Point> points = new ArrayList<>();
        for (int r = 0; r < getRows(); r++) {
            for (int index = 0; index < getRow(r).getSize(); index++) {
                points.add(new Point(r, rowsList.get(r).getValue(index)));
            }
        }
        return points;
    }

    /**
     * 从一个list<SparseArray> 中根据指定边界提取子list
     *
     * @param originList      被提取的list
     * @param startListIndex  从originList的startListIndex 行 开始 提取
     * @param endListIndex    一直提取到originList 的 endListIndex 行 （不含）
     * @param startArrayIndex 被提取的行中，被提取元素值的边界。例如 被提取行存储了[2，3，4，5，7，9]， startArrayIndex 为 4，
     *                        endArrayIndex 为 6， 则从该行提取的数组为 [4,5]
     * @param endArrayIndex   见上行
     * @return 提取得到的list
     */
    private static ArrayList<SparseVector> getSubSparseList(List<SparseVector> originList,
                                                            int startListIndex, int endListIndex, int startArrayIndex, int endArrayIndex) {
        IntStream intStream = IntStream.range(startListIndex, endListIndex);
        return intStream.parallel().
                mapToObj(index -> originList.get(index).getSubArray(startArrayIndex, endArrayIndex)
                ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 计算 矩阵 y = x1+x2。其中 x1和x2 为按照行存储或者按照列存储的List<SparseArray>。
     *
     * @param x1 list1
     * @param x2 list2
     * @param y  加和结果
     */
    private static void sparseListAddI(List<SparseVector> x1, List<SparseVector> x2, List<SparseVector> y) {
        for (int i = 0; i < x1.size(); ++i) {
            y.add(x1.get(i).addSparseArray(x2.get(i)));
        }
    }
    /*
     *****************************************************
     * SparseMtx类内部static方法*****END******
     * ***************************************************
     */


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SparseMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        SparseMatrix that = (SparseMatrix) obj;
        if (this.type != that.type) {
            return false;
        }
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        switch (type) {
            case Full: {
                equalsBuilder = equalsBuilder.append(this.rowsList, that.rowsList).
                        append(this.colsList, that.colsList).append(this.rows, that.rows).
                        append(this.cols, that.cols);
                break;
            }
            case RowsOnly: {
                equalsBuilder = equalsBuilder.append(this.rowsList, that.rowsList).
                        append(this.rows, that.rows).
                        append(this.cols, that.cols);
                break;
            }
            case ColsOnly: {
                equalsBuilder = equalsBuilder.append(this.colsList, that.colsList).
                        append(this.rows, that.rows).
                        append(this.cols, that.cols);
            }
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(rowsList).append(colsList).toHashCode();
    }

    /**
     * Point类，每个Point由行坐标和列坐标定义
     */
    public static class Point {
        /**
         * 行坐标
         */
        public int rowIndex;
        /**
         * 列坐标
         */
        public int colIndex;

        /**
         * 构造函数
         *
         * @param row 行坐标
         * @param col 列坐标
         */
        public Point(int row, int col) {
            rowIndex = row;
            colIndex = col;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Point)) {
                return false;
            }
            if (this == obj) {
                return true;
            }

            Point that = (Point) obj;
            return new EqualsBuilder().append(this.rowIndex, that.rowIndex).append(this.colIndex, that.colIndex).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(rowIndex).append(colIndex).toHashCode();
        }
    }

    public static class PointList {
        /**
         * 用于存储点列的arrarylist
         */
        public ArrayList<Point> pointsArrayList;
        /**
         * 该点列对应矩阵的行数和列数，也限制了每一点坐标的取值范围
         */
        int maxRowIndex, maxColIndex;

        /**
         * 构造函数
         *
         * @param rows 指定的行数
         * @param cols 指定的列数
         */
        public PointList(int rows, int cols) {
            maxRowIndex = rows;
            maxColIndex = cols;
            pointsArrayList = new ArrayList<>(maxColIndex);
        }

        public PointList(int rows, int cols, ArrayList<Point> pp) {
            maxRowIndex = rows;
            maxColIndex = cols;
            pointsArrayList = new ArrayList<>();
            pointsArrayList.addAll(pp);
        }

        /**
         * 添加点到PointList 当中
         *
         * @param p 需要添加的点
         */
        public void addPoint(Point p) {
            // 被添加点的坐标需要在Pointlist的边界内。
            assert (p.rowIndex < maxRowIndex);
            assert (p.colIndex < maxColIndex);
            pointsArrayList.add(p);
        }

        /**
         * 返回ArrayList
         *
         * @return pointsArrayList
         */
        public ArrayList<Point> getPoints() {
            return pointsArrayList;
        }
    }

}


