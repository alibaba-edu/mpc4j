package edu.alibaba.mpc4j.sml.opboost.xgboost;

import biz.k11i.xgboost.fvec.Fvec;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import smile.data.DataFrame;
import smile.data.formula.Formula;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * OpXgBoost工具类。
 *
 * @author Weiran Liu
 * @date 2021/10/09
 */
public class OpXgBoostUtils {

    private OpXgBoostUtils() {
        // empty
    }

    /**
     * 树训练类型
     */
    public enum TreeMethodType {
        /**
         * Use heuristic to choose the fastest method.
         * - For small dataset, exact greedy (exact) will be used.
         * - For larger dataset, approximate algorithm (approx) will be chosen.
         */
        AUTO,
        /**
         * Exact greedy algorithm. Enumerates all split candidates.
         */
        EXACT,
        /**
         * Approximate greedy algorithm using quantile sketch and gradient histogram.
         */
        ARRPOX,
    }

    /**
     * 将数据帧（DataFrame）转换为数据矩阵（DMatrix）。
     *
     * @param formula   标签。
     * @param dataFrame 数据帧。
     * @return 数据矩阵。
     * @throws XGBoostError 如果出现转换异常。
     */
    public static DMatrix dataFrameToDataMatrix(Formula formula, DataFrame dataFrame) throws XGBoostError {
        DataFrame x = formula.x(dataFrame);
        DMatrix dMatrix = dataFrameToDataMatrix(x);
        // 编码预测数据
        double[] doubleLabels = formula.y(dataFrame).toDoubleArray();
        float[] floatLabels = new float[doubleLabels.length];
        IntStream.range(0, doubleLabels.length).forEach(index -> floatLabels[index] = (float) doubleLabels[index]);
        dMatrix.setLabel(floatLabels);
        return dMatrix;
    }

    private static DMatrix dataFrameToDataMatrix(DataFrame dataFrame) throws XGBoostError {
        double[][] dataFrameArray = dataFrame.toArray();
        int rowNum = dataFrameArray.length;
        assert rowNum > 0 : "DataFrame must contain at least one row: " + rowNum;
        int columnNum = dataFrameArray[0].length;
        Arrays.stream(dataFrameArray).forEach(row -> {
            assert row.length == columnNum : "All rows must have same columns " + columnNum + ": " + row.length;
        });
        // 创建临时变量
        float[] dataMatrixArray = new float[rowNum * columnNum];
        IntStream.range(0, rowNum).forEach(row ->
            IntStream.range(0, columnNum).forEach(column ->
                dataMatrixArray[row * columnNum + column] = (float) dataFrameArray[row][column]
            )
        );
        return new DMatrix(dataMatrixArray, rowNum, columnNum, 0);
    }

    /**
     * 将数据帧（DataFrame）转换为特征向量（FeatureVector）。
     *
     * @param dataFrame 数据帧。
     * @return 特征向量。
     */
    public static Fvec[] dataFrameToFeatureVector(DataFrame dataFrame) {
        double[][] dataFrameArray = dataFrame.toArray();
        return Arrays.stream(dataFrameArray)
            .map(doubles -> Fvec.Transformer.fromArray(doubles, false))
            .toArray(Fvec[]::new);
    }
}
