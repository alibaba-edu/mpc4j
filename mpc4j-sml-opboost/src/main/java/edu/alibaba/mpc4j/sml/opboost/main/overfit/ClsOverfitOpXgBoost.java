package edu.alibaba.mpc4j.sml.opboost.main.overfit;

import biz.k11i.xgboost.Predictor;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.OpBoostUtils;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostLdpType;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostTaskType;
import edu.alibaba.mpc4j.sml.opboost.xgboost.OpXgBoostUtils;
import edu.alibaba.mpc4j.sml.opboost.xgboost.XgBoostClsParams;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import smile.data.DataFrame;
import smile.validation.metric.AUC;
import smile.validation.metric.Accuracy;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 分类过拟合OpXgBoost测试。
 *
 * @author Weiran Liu
 * @date 2022/7/21
 */
public class ClsOverfitOpXgBoost extends AbstractOverfitOpXgBoost {
    /**
     * 分类数量
     */
    private int numClass;
    /**
     * 训练参数
     */
    private Map<String, Object> trainParams;
    /**
     * 训练数据真实值
     */
    private int[] trainTruths;
    /**
     * 测试数据真实值
     */
    private int[] testTruths;

    public ClsOverfitOpXgBoost(Properties properties) {
        super(properties, OpBoostTaskType.CLS_OVERFIT_OP_XG_BOOST);
    }

    @Override
    public void init() throws IOException, URISyntaxException {
        super.init();
        numClass = OpBoostUtils.getNumClass(formula, trainDataFrame);
        trainParams = new XgBoostClsParams.Builder(numClass)
            .setTreeNum(treeNum)
            .setMaxDepth(maxDepth)
            .setShrinkage(shrinkage)
            .build()
            .getParams();
        trainTruths = formula.y(trainDataFrame).toIntArray();
        testTruths = formula.y(testDataFrame).toIntArray();
    }

    @Override
    public void run() throws IOException, XGBoostError, MpcAbortException {
        String filePath = taskType
            // 数据集名称
            + "_" + datasetName
            // 测试轮数
            + "_" + totalRound
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 输出表头
        String tab = "name\tε\tθ\tα\tTreeNum\tLDP Train Measure\tTrain Measure\tTest Measure";
        printWriter.println(tab);
        // 明文训练
        runPlainTraining(printWriter);
        // 分段
        runEpsilonLdpTraining(printWriter, OpBoostLdpType.PIECEWISE);
        // GlobalMap
        runEpsilonLdpTraining(printWriter, OpBoostLdpType.GLOBAL_MAP);
        // GlobalExpMap
        runEpsilonLdpTraining(printWriter, OpBoostLdpType.GLOBAL_EXP_MAP);
        // LocalMap
        runLocalMapLdpTraining(printWriter, OpBoostLdpType.LOCAL_MAP);
        // LocalExpMap
        runLocalMapLdpTraining(printWriter, OpBoostLdpType.LOCAL_EXP_MAP);
        // AdjMap
        runAdjMapLdpTraining(printWriter, OpBoostLdpType.ADJ_MAP);
        // AdjExpMap
        runAdjMapLdpTraining(printWriter, OpBoostLdpType.ADJ_EXP_MAP);
        // 清理状态
        printWriter.close();
        fileWriter.close();
    }

    private void runPlainTraining(PrintWriter printWriter) throws XGBoostError, IOException {
        LOGGER.info("-----{} for {}-----", OpBoostLdpType.PLAIN.name(), taskType);
        double[] trainMeasures = new double[treeNum];
        double[] testMeasures = new double[treeNum];
        DMatrix trainDataMatrix = OpXgBoostUtils.dataFrameToDataMatrix(formula, trainDataFrame);
        Booster booster = XGBoost.train(trainDataMatrix, trainParams, treeNum, new HashMap<>(0), null, null);
        String modelName = taskType + "_PLAIN.deprecated";
        booster.saveModel(modelName);
        File modelFile = new File(modelName);
        FileInputStream fileInputStream = new FileInputStream(modelFile);
        Predictor model = new Predictor(fileInputStream);
        fileInputStream.close();
        modelFile.deleteOnExit();
        if (numClass == 2) {
            // 如果是二分类，则记录AUC
            for (int index = 0; index < treeNum; index++) {
                int treeLimit = index + 1;
                double[] trainProbabilities = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainFeatureDataFrame))
                    .parallel()
                    .map(fvec -> model.predict(fvec, false, treeLimit))
                    .mapToDouble(floats -> floats[0])
                    .toArray();
                double trainMeasure = AUC.of(trainTruths, trainProbabilities);
                double[] testProbabilities = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testFeatureDataFrame))
                    .parallel()
                    .map(fvec -> model.predict(fvec, false, treeLimit))
                    .mapToDouble(floats -> floats[0])
                    .toArray();
                double testMeasure = AUC.of(testTruths, testProbabilities);
                LOGGER.info("PLAIN: Tree = {}, Train AUC = {}, Test AUC = {}", treeLimit, trainMeasure, testMeasure);
                trainMeasures[index] = trainMeasure;
                testMeasures[index] = testMeasure;
            }
        } else {
            // 如果是多分类问题，则计算准确率
            for (int index = 0; index < treeNum; index++) {
                int treeLimit = index + 1;
                int[] trainPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainFeatureDataFrame))
                    .parallel()
                    .map(fvec -> model.predict(fvec, false, treeLimit))
                    .mapToInt(floats -> Math.round(floats[0]))
                    .toArray();
                double trainMeasure = Accuracy.of(trainTruths, trainPredicts);
                int[] testPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testFeatureDataFrame))
                    .parallel()
                    .map(fvec -> model.predict(fvec, false, treeLimit))
                    .mapToInt(floats -> Math.round(floats[0]))
                    .toArray();
                double testMeasure = Accuracy.of(testTruths, testPredicts);
                LOGGER.info("Tree = {}, Train Acc. = {}, Test Acc. = {}", treeLimit, trainMeasure, testMeasure);
                trainMeasures[index] = trainMeasure;
                testMeasures[index] = testMeasure;
            }
        }
        for (int index = 0; index < treeNum; index++) {
            int treeLimit = index + 1;
            writeInfo(printWriter, OpBoostLdpType.PLAIN.name(), null, null, null, treeLimit,
                null, trainMeasures[index], testMeasures[index]
            );
        }
    }

    private void runEpsilonLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType) throws XGBoostError, IOException {
        for (double epsilon : epsilons) {
            LOGGER.info("-----{} for {}: ε = {}-----", ldpType.name(), taskType, epsilon);
            Map<String, LdpConfig> ldpConfigMap = createLdpConfigs(ldpType, epsilon);
            double[][] measures = runLdpTraining(ldpConfigMap);
            for (int index = 0; index < treeNum; index++) {
                int tree = index + 1;
                writeInfo(printWriter, ldpType.name(), epsilon, null, null, tree,
                    measures[0][index], measures[1][index], measures[2][index]
                );
            }
        }
    }

    private void runLocalMapLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType) throws XGBoostError, IOException {
        for (int theta : thetas) {
            for (double epsilon : epsilons) {
                LOGGER.info("-----{} for {}: ε = {}, θ = {}-----", ldpType.name(), taskType, epsilon, theta);
                Map<String, LdpConfig> ldpConfigMap = createLdpConfigs(ldpType, epsilon, theta);
                double[][] measures = runLdpTraining(ldpConfigMap);
                for (int index = 0; index < treeNum; index++) {
                    int tree = index + 1;
                    writeInfo(printWriter, ldpType.name(), epsilon, theta, null, tree,
                        measures[0][index], measures[1][index], measures[2][index]
                    );
                }
            }
        }
    }

    private void runAdjMapLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType) throws XGBoostError, IOException {
        for (double alpha : alphas) {
            for (int theta : thetas) {
                for (double epsilon : epsilons) {
                    LOGGER.info("-----{} for {}: ε = {}, θ = {}, α = {}-----", ldpType.name(), taskType, epsilon, theta, alpha);
                    Map<String, LdpConfig> ldpConfigMap = createLdpConfigs(ldpType, epsilon, theta, alpha);
                    double[][] measures = runLdpTraining(ldpConfigMap);
                    for (int index = 0; index < treeNum; index++) {
                        int tree = index + 1;
                        writeInfo(printWriter, ldpType.name(), epsilon, theta, alpha, tree,
                            measures[0][index], measures[1][index], measures[2][index]
                        );
                    }
                }
            }
        }
    }

    private double[][] runLdpTraining(Map<String, LdpConfig> ldpConfigMap) throws XGBoostError, IOException {
        double[][] measures = new double[3][treeNum];
        for (int round = 1; round <= totalRound; round++) {
            DataFrame ldpTrainDataFrame = OpBoostUtils.ldpDataFrame(trainDataFrame, ldpConfigMap);
            DataFrame ldpTrainFeatureDataFrame = formula.x(ldpTrainDataFrame);
            // 用LDP后的数据进行训练
            DMatrix ldpTrainDataMatrix = OpXgBoostUtils.dataFrameToDataMatrix(formula, ldpTrainDataFrame);
            Booster booster = XGBoost.train(ldpTrainDataMatrix, trainParams, treeNum, new HashMap<>(0), null, null);
            String modelName = taskType + "_" + round + ".deprecated";
            booster.saveModel(modelName);
            File modelFile = new File(modelName);
            FileInputStream fileInputStream = new FileInputStream(modelFile);
            Predictor model = new Predictor(fileInputStream);
            fileInputStream.close();
            modelFile.deleteOnExit();
            if (numClass == 2) {
                // 如果是二分类，则记录AUC
                for (int index = 0; index < treeNum; index++) {
                    int treeLimit = index + 1;
                    double[] ldpTrainPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(ldpTrainFeatureDataFrame))
                        .parallel()
                        .map(fvec -> model.predict(fvec, false, treeLimit))
                        .mapToDouble(floats -> floats[0])
                        .toArray();
                    double ldpTrainMeasure = AUC.of(trainTruths, ldpTrainPredicts);
                    double[] trainPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainFeatureDataFrame))
                        .parallel()
                        .map(fvec -> model.predict(fvec, false, treeLimit))
                        .mapToDouble(floats -> floats[0])
                        .toArray();
                    double trainMeasure = AUC.of(trainTruths, trainPredicts);
                    double[] testPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testFeatureDataFrame))
                        .parallel()
                        .map(fvec -> model.predict(fvec, false, treeLimit))
                        .mapToDouble(floats -> floats[0])
                        .toArray();
                    double testMeasure = AUC.of(testTruths, testPredicts);
                    LOGGER.info("Round {}: Tree = {}, LDP Train AUC = {}, Train AUC = {}, Test AUC = {}",
                        round, treeLimit, ldpTrainMeasure, trainMeasure, testMeasure);
                    measures[0][index] += ldpTrainMeasure;
                    measures[1][index] += trainMeasure;
                    measures[2][index] += testMeasure;
                }
            } else {
                // 如果是多分类问题，则计算准确率
                for (int index = 0; index < treeNum; index++) {
                    int treeLimit = index + 1;
                    int[] ldpTrainPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(ldpTrainFeatureDataFrame))
                        .parallel()
                        .map(fvec -> model.predict(fvec, false, treeLimit))
                        .mapToInt(floats -> Math.round(floats[0]))
                        .toArray();
                    double ldpTrainMeasure = Accuracy.of(trainTruths, ldpTrainPredicts);
                    int[] trainPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainFeatureDataFrame))
                        .parallel()
                        .map(fvec -> model.predict(fvec, false, treeLimit))
                        .mapToInt(floats -> Math.round(floats[0]))
                        .toArray();
                    double trainMeasure = Accuracy.of(trainTruths, trainPredicts);
                    int[] testPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testFeatureDataFrame))
                        .parallel()
                        .map(fvec -> model.predict(fvec, false, treeLimit))
                        .mapToInt(floats -> Math.round(floats[0]))
                        .toArray();
                    double testMeasure = Accuracy.of(testTruths, testPredicts);
                    LOGGER.info("Round {}: Tree = {}, LDP Train Acc. = {}, Train Acc. = {}, Test Acc. = {}",
                        round, treeLimit, ldpTrainMeasure, trainMeasure, testMeasure);
                    measures[0][index] += ldpTrainMeasure;
                    measures[1][index] += trainMeasure;
                    measures[2][index] += testMeasure;
                }
            }
        }
        // 所有度量值除以轮数
        for (int index = 0; index < treeNum; index++) {
            measures[0][index] = measures[0][index] / totalRound;
            measures[1][index] = measures[1][index] / totalRound;
            measures[2][index] = measures[2][index] / totalRound;
        }
        return measures;
    }
}
