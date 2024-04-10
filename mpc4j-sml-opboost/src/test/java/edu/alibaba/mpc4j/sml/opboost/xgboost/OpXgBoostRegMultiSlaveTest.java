package edu.alibaba.mpc4j.sml.opboost.xgboost;

import biz.k11i.xgboost.Predictor;
import edu.alibaba.mpc4j.common.data.DataFrameUtils;
import edu.alibaba.mpc4j.common.data.DatasetManager;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlave;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlaveConfig;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlaveThread;
import edu.alibaba.mpc4j.sml.opboost.OpBoostTestUtils;
import edu.alibaba.mpc4j.common.data.regression.*;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.apache.commons.math3.util.Precision;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.validation.metric.MSE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

/**
 * 回归OpXgBoost多从机测试。
 * <p>
 * 多从机时，Abalone和BostonHousing的明文测试结果与预期不相符。
 * </p>
 * <p>
 * Abalone数据集中，只有（从0计数）的第236个数据项，其明文联邦模型预测结果与明文单机模型预测结果不一致，具体原因不明。
 * </p>
 * <p>
 * BostonHousing数据集中，（从0计数）第15个树（叶子）节点和第16个树（叶子）节点的权重值，其明文联邦模型与明文单机模型不一致。
 * - 第15个树节点：明文单机模型6.642614，明文联邦模型5.8780646。
 * - 第16个树节点：明文单机模型8.017501，明文联邦模型7.5159636。
 * 这两个树节点的父节点为第8个树节点，其切分列索引值为5。
 * - 明文切分条件为6.543。
 * - 联邦切分点为第194个排序值和第195个排序值之间，替换后的切分值为6.1245。
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/10/08
 */
@RunWith(Parameterized.class)
public class OpXgBoostRegMultiSlaveTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpXgBoostRegMultiSlaveTest.class);

    static {
        DatasetManager.setPathPrefix("../data/");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CPU样本集，一个小样本集，容易查找错误
        configurations.add(new Object[]{"CPU", Cpu.formula, Cpu.data, Cpu.data, true});
        // Abalone样本集，左侧包含枚举值，右侧均为连续值
        configurations.add(new Object[]{"Abalone", Abalone.formula, Abalone.train, Abalone.test, false});
        // AutoMpg样本集，左侧右侧均包含枚举值，且右侧枚举值不从0开始
        configurations.add(new Object[]{"autoMPG", AutoMpg.formula, AutoMpg.data, AutoMpg.data, true});
        // Housing样本集，包含枚举值和连续值，CHAS只有2种可能的枚举取值
        configurations.add(
            new Object[]{"BostonHousing", BostonHousing.formula, BostonHousing.data, BostonHousing.data, false}
        );
        // kin8nm样本集，数据带负数
        configurations.add(new Object[]{"kin8nm", Kin8nm.formula, Kin8nm.data, Kin8nm.data, true});

        return configurations;
    }

    /**
     * 数据集名称
     */
    private final String name;
    /**
     * 标签
     */
    private final Formula formula;
    /**
     * 训练数据
     */
    private final DataFrame train;
    /**
     * 测试数据
     */
    private final DataFrame test;
    /**
     * 明文测试是否验证一致性
     */
    private final boolean plainVerify;
    /**
     * 主机
     */
    private final OpXgBoostHost host;
    /**
     * 左从机
     */
    private final OpBoostSlave leftSlave;
    /**
     * 右从机
     */
    private final OpBoostSlave rightSlave;

    public OpXgBoostRegMultiSlaveTest(String name, Formula formula, DataFrame train, DataFrame test, boolean plainVerify) {
        super(name);
        this.name = name;
        this.formula = formula;
        this.train = train;
        this.test = test;
        this.plainVerify = plainVerify;
        host = new OpXgBoostHost(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty());
        leftSlave = new OpBoostSlave(secondRpc, firstRpc.ownParty());
        rightSlave = new OpBoostSlave(thirdRpc, firstRpc.ownParty());
    }

    @Before
    @Override
    public void connect() {
        super.connect();
        host.init();
        leftSlave.init();
        rightSlave.init();
    }

    @After
    @Override
    public void disconnect() {
        host.destroy();
        leftSlave.destroy();
        rightSlave.destroy();
        super.disconnect();
    }

    @Test
    public void testLargeEpsilonLdpTraining() {
        testLdpTraining(OpBoostTestUtils.LARGE_EPSILON);
    }

    @Test
    public void testDefaultEpsilonLdpTraining() {
        testLdpTraining(OpBoostTestUtils.DEFAULT_EPSILON);
    }

    @Test
    public void testSmallEpsilonLdpTraining() {
        testLdpTraining(OpBoostTestUtils.SMALL_EPSILON);
    }

    private void testLdpTraining(double epsilon) {
        LOGGER.info("----{} noise training, ε = {}-----", name, epsilon);
        XgBoostParams xgBoostParams = new XgBoostRegParams.Builder().build();
        // 明文训练
        Predictor plainModel = plainTraining(formula, train, xgBoostParams);
        // 联邦训练
        DataFrame[] splitDataFrame = DataFrameUtils.split(formula.x(train), 3);
        DataFrame hostDataFrame = splitDataFrame[0].merge(formula.y(train));
        Map<String, LdpConfig> hostLdpConfigMap = OpBoostTestUtils.createLdpConfigMap(splitDataFrame[0], epsilon);
        OpXgBoostHostConfig hostConfig = new OpXgBoostHostConfig
            .Builder(hostDataFrame.schema(), xgBoostParams)
            .addLdpConfig(hostLdpConfigMap)
            .build();
        DataFrame leftSlaveDataFrame = splitDataFrame[1];
        Map<String, LdpConfig> leftSlaveLdpConfigMap = OpBoostTestUtils.createLdpConfigMap(leftSlaveDataFrame, epsilon);
        OpBoostSlaveConfig leftSlaveConfig = new OpBoostSlaveConfig
            .Builder(leftSlaveDataFrame.schema())
            .addLdpConfig(leftSlaveLdpConfigMap)
            .build();
        DataFrame rightSlaveDataFrame = splitDataFrame[2];
        Map<String, LdpConfig> rightSlaveLdpConfigMap = OpBoostTestUtils.createLdpConfigMap(rightSlaveDataFrame, epsilon);
        OpBoostSlaveConfig rightSlaveConfig = new OpBoostSlaveConfig
            .Builder(rightSlaveDataFrame.schema())
            .addLdpConfig(rightSlaveLdpConfigMap)
            .build();
        Predictor federatedModel = federateTraining(formula,
            hostDataFrame, hostConfig, leftSlaveDataFrame, leftSlaveConfig, rightSlaveDataFrame, rightSlaveConfig
        );
        LOGGER.info("{} verify difference between plain model and federated model", name);
        // 预测结果
        double[] truths = formula.y(test).toDoubleArray();
        DataFrame testX = formula.x(test);
        double[] plainModelPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testX))
            .map(plainModel::predict)
            .mapToDouble(floats -> floats[0])
            .toArray();
        double[] federatedModelPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testX))
            .map(federatedModel::predict)
            .mapToDouble(floats -> floats[0])
            .toArray();
        // 计算MSE
        double plainPredictMse = MSE.of(truths, plainModelPredicts);
        double federatedPredictMse = MSE.of(truths, federatedModelPredicts);
        LOGGER.info("ε = {}，plain MSE = {}，federated MSE = {}", epsilon, plainPredictMse, federatedPredictMse);
    }

    @Test
    public void testPlainTraining() {
        LOGGER.info("----{} plain training-----", name);
        XgBoostParams xgBoostParams = new XgBoostRegParams.Builder().build();
        // 明文训练
        Predictor plainModel = plainTraining(formula, train, xgBoostParams);
        // 联邦训练
        DataFrame[] splitDataFrame = DataFrameUtils.split(formula.x(train), 3);
        DataFrame hostDataFrame = splitDataFrame[0].merge(formula.y(train));
        OpXgBoostHostConfig hostConfig = new OpXgBoostHostConfig
            .Builder(hostDataFrame.schema(), xgBoostParams)
            .build();
        DataFrame leftSlaveDataFrame = splitDataFrame[1];
        OpBoostSlaveConfig leftSlaveConfig = new OpBoostSlaveConfig.Builder(leftSlaveDataFrame.schema()).build();
        DataFrame rightSlaveDataFrame = splitDataFrame[2];
        OpBoostSlaveConfig rightSlaveConfig = new OpBoostSlaveConfig.Builder(rightSlaveDataFrame.schema()).build();
        Predictor federatedModel = federateTraining(formula,
            hostDataFrame, hostConfig, leftSlaveDataFrame, leftSlaveConfig, rightSlaveDataFrame, rightSlaveConfig
        );
        // 无噪声决策树对于训练数据的预测结果应该完全相同
        DataFrame trainX = formula.x(train);
        double[] plainPredictions = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainX))
            .map(plainModel::predict)
            .mapToDouble(floats -> floats[0])
            .toArray();
        double[] federatedPredictions = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainX))
            .map(federatedModel::predict)
            .mapToDouble(floats -> floats[0])
            .toArray();
        Assert.assertEquals(plainPredictions.length, federatedPredictions.length);
        if (plainVerify) {
            // 明文训练验证预测结果完全一致性
            LOGGER.info("{} verify same prediction for train data", name);
            Assert.assertArrayEquals(plainPredictions, federatedPredictions, DoubleUtils.PRECISION);
        } else {
            // 特殊数据集统计预测结果一致的数量
            LOGGER.info("{} counting same prediction for train data", name);
            int totalNum = plainPredictions.length;
            long sameNum = IntStream.range(0, totalNum)
                .filter(index -> Precision.equals(plainPredictions[index], federatedPredictions[index], DoubleUtils.PRECISION))
                .count();
            LOGGER.info("total_num = {}, same_num = {}", totalNum, sameNum);
        }
    }

    private Predictor plainTraining(Formula formula, DataFrame data, XgBoostParams xgBoostParams) {
        try {
            Map<String, Object> params = xgBoostParams.getParams();
            int treeNum = xgBoostParams.getTreeNum();
            DMatrix trainDataMatrix = OpXgBoostUtils.dataFrameToDataMatrix(formula, data);
            Booster booster = XGBoost.train(trainDataMatrix, params, treeNum, new HashMap<>(), null, null);
            String modelName = name + "_plain.deprecated";
            booster.saveModel(modelName);
            File modelFile = new File(modelName);
            FileInputStream fileInputStream = new FileInputStream(modelFile);
            Predictor predictor = new Predictor(fileInputStream);
            fileInputStream.close();
            // 删除模型
            modelFile.deleteOnExit();
            return predictor;
        } catch (XGBoostError | IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Inner Error...");
        }
    }

    private Predictor federateTraining(Formula formula,
                                       DataFrame hostDataFrame, OpXgBoostHostConfig hostConfig,
                                       DataFrame leftSlaveDataFrame, OpBoostSlaveConfig leftSlaveConfig,
                                       DataFrame rightSlaveDataFrame, OpBoostSlaveConfig rightSlaveConfig
    ) {
        int randomTaskId = Math.abs(OpBoostTestUtils.SECURE_RANDOM.nextInt());
        host.setTaskId(randomTaskId);
        leftSlave.setTaskId(randomTaskId);
        rightSlave.setTaskId(randomTaskId);
        try {
            OpXgBoostHostThread hostThread = new OpXgBoostHostThread(host, formula, hostDataFrame, hostConfig);
            OpBoostSlaveThread leftSlaveThread = new OpBoostSlaveThread(leftSlave, leftSlaveDataFrame, leftSlaveConfig);
            OpBoostSlaveThread rightSlaveThread = new OpBoostSlaveThread(rightSlave, rightSlaveDataFrame, rightSlaveConfig);
            // 开始执行协议
            hostThread.start();
            leftSlaveThread.start();
            rightSlaveThread.start();
            // 等待线程停止
            hostThread.join();
            leftSlaveThread.join();
            rightSlaveThread.join();
            // 返回模型
            return hostThread.getModel();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Error for " + name);
        }
    }
}
