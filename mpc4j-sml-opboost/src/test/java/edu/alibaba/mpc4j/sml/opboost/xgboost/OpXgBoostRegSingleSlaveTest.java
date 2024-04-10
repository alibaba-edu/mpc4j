package edu.alibaba.mpc4j.sml.opboost.xgboost;

import biz.k11i.xgboost.Predictor;
import edu.alibaba.mpc4j.common.data.DataFrameUtils;
import edu.alibaba.mpc4j.common.data.DatasetManager;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
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

/**
 * 回归OpXgBoost单从机测试。
 *
 * @author Weiran Liu
 * @date 2021/10/08
 */
@RunWith(Parameterized.class)
public class OpXgBoostRegSingleSlaveTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpXgBoostRegSingleSlaveTest.class);

    static {
        DatasetManager.setPathPrefix("../data/");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CPU样本集，一个小样本集，容易查找错误
        configurations.add(new Object[]{"CPU", Cpu.formula, Cpu.data, Cpu.data,});
        // Abalone样本集，左侧包含枚举值，右侧均为连续值
        configurations.add(new Object[]{"Abalone", Abalone.formula, Abalone.train, Abalone.test});
        // AutoMpg样本集，左侧右侧均包含枚举值，且右侧枚举值不从0开始
        configurations.add(new Object[]{"autoMPG", AutoMpg.formula, AutoMpg.data, AutoMpg.data});
        // Housing样本集，包含枚举值和连续值，CHAS只有2种可能的枚举取值
        configurations.add(new Object[]{"BostonHousing", BostonHousing.formula, BostonHousing.data, BostonHousing.data});
        // kin8nm样本集，数据带负数
        configurations.add(new Object[]{"kin8nm", Kin8nm.formula, Kin8nm.data, Kin8nm.data});

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
     * 主机
     */
    private final OpXgBoostHost host;
    /**
     * 从机
     */
    private final OpBoostSlave slave;

    public OpXgBoostRegSingleSlaveTest(String name, Formula formula, DataFrame train, DataFrame test) {
        super(name);
        this.name = name;
        this.formula = formula;
        this.train = train;
        this.test = test;
        host = new OpXgBoostHost(firstRpc, secondRpc.ownParty());
        slave = new OpBoostSlave(secondRpc, firstRpc.ownParty());
    }

    @Before
    @Override
    public void connect() {
        super.connect();
        host.init();
        slave.init();
    }

    @After
    @Override
    public void disconnect() {
        host.destroy();
        slave.destroy();
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
        LOGGER.info("----{} LDP training, ε = {}-----", name, epsilon);
        XgBoostParams xgBoostParams = new XgBoostRegParams.Builder().build();
        // 明文训练
        Predictor plainModel = plainTraining(formula, train, xgBoostParams);
        // 联邦训练
        DataFrame[] splitDataFrame = DataFrameUtils.split(formula.x(train), 2);
        DataFrame hostDataFrame = splitDataFrame[0].merge(formula.y(train));
        Map<String, LdpConfig> hostLdpConfigMap = OpBoostTestUtils.createLdpConfigMap(splitDataFrame[0], epsilon);
        OpXgBoostHostConfig hostConfig = new OpXgBoostHostConfig
            .Builder(hostDataFrame.schema(), xgBoostParams)
            .addLdpConfig(hostLdpConfigMap)
            .build();
        DataFrame slaveDataFrame = splitDataFrame[1];
        Map<String, LdpConfig> slaveLdpConfigMap = OpBoostTestUtils.createLdpConfigMap(slaveDataFrame, epsilon);
        OpBoostSlaveConfig slaveConfig = new OpBoostSlaveConfig
            .Builder(slaveDataFrame.schema())
            .addLdpConfig(slaveLdpConfigMap)
            .build();
        Predictor federatedModel = federateTraining(formula, hostDataFrame, hostConfig, slaveDataFrame, slaveConfig);
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
        DataFrame[] splitDataFrame = DataFrameUtils.split(formula.x(train), 2);
        DataFrame hostDataFrame = splitDataFrame[0].merge(formula.y(train));
        OpXgBoostHostConfig hostConfig = new OpXgBoostHostConfig
            .Builder(hostDataFrame.schema(), xgBoostParams)
            .build();
        DataFrame slaveDataFrame = splitDataFrame[1];
        OpBoostSlaveConfig slaveConfig = new OpBoostSlaveConfig.Builder(slaveDataFrame.schema()).build();
        Predictor federatedModel = federateTraining(formula, hostDataFrame, hostConfig, slaveDataFrame, slaveConfig);
        // 明文训练验证预测结果完全一致性
        LOGGER.info("{} verify same prediction for train data", name);
        DataFrame trainX = formula.x(train);
        double[] plainPredictions = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainX))
            .map(plainModel::predict)
            .mapToDouble(floats -> floats[0])
            .toArray();
        double[] federatedPredictions = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainX))
            .map(federatedModel::predict)
            .mapToDouble(floats -> floats[0])
            .toArray();
        Assert.assertArrayEquals(plainPredictions, federatedPredictions, DoubleUtils.PRECISION);
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
                                       DataFrame slaveDataFrame, OpBoostSlaveConfig slaveConfig) {
        int randomTaskId = Math.abs(OpBoostTestUtils.SECURE_RANDOM.nextInt());
        host.setTaskId(randomTaskId);
        slave.setTaskId(randomTaskId);
        try {
            OpXgBoostHostThread hostThread = new OpXgBoostHostThread(host, formula, hostDataFrame, hostConfig);
            OpBoostSlaveThread slaveThread = new OpBoostSlaveThread(slave, slaveDataFrame, slaveConfig);
            // 开始执行协议
            hostThread.start();
            slaveThread.start();
            // 等待线程停止
            hostThread.join();
            slaveThread.join();
            // 返回模型
            return hostThread.getModel();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Error for " + name);
        }
    }
}
