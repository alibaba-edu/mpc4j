package edu.alibaba.mpc4j.sml.opboost.xgboost;

import biz.k11i.xgboost.Predictor;
import edu.alibaba.mpc4j.common.data.DataFrameUtils;
import edu.alibaba.mpc4j.common.data.DatasetManager;
import edu.alibaba.mpc4j.common.data.classification.BreastCancer;
import edu.alibaba.mpc4j.common.data.classification.Iris;
import edu.alibaba.mpc4j.common.data.classification.PenDigits;
import edu.alibaba.mpc4j.common.data.classification.Weather;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.*;
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
import smile.validation.metric.Accuracy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

/**
 * OpXgBoost分类测试。
 * <p>
 * 多从机时，PenDigits的明文测试共有7494个数据，其中有7418个结果相同，76个结果不同，具体原因不明。
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/10/09
 */
@RunWith(Parameterized.class)
public class OpXgBoostClsMultiSlaveTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpXgBoostClsMultiSlaveTest.class);

    static {
        DatasetManager.setPathPrefix("../data/");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // 两分类任务，特征均为枚举值
        configurations.add(new Object[]{"Weather", Weather.formula, Weather.data, Weather.data, true});
        // 三分类任务，特征均为float
        configurations.add(new Object[]{"Iris", Iris.formula, Iris.data, Iris.data, true});
        // 多分类任务，特征均为double
        configurations.add(new Object[]{"PenDigits", PenDigits.formula, PenDigits.data, PenDigits.data, false});
        // 二分类任务，较大规模数据
        configurations.add(
            new Object[]{"BreastCancer", BreastCancer.formula, BreastCancer.data, BreastCancer.data, true}
        );

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

    public OpXgBoostClsMultiSlaveTest(String name, Formula formula, DataFrame train, DataFrame test, boolean plainVerify) {
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
        XgBoostParams xgBoostParams = new XgBoostClsParams.Builder(OpBoostUtils.getNumClass(formula, train)).build();
        // 明文训练
        Predictor plainModel = plainTraining(name, formula, train, xgBoostParams);
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

        Predictor federatedModel = federateTraining(name, formula,
            hostDataFrame, hostConfig, leftSlaveDataFrame, leftSlaveConfig, rightSlaveDataFrame, rightSlaveConfig
        );
        LOGGER.info("{} verify difference between plain model and federated model", name);
        // 预测结果
        int[] truths = formula.y(test).toIntArray();
        DataFrame testX = formula.x(test);
        int[] plainModelPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testX))
            .map(plainModel::predict)
            .mapToInt(floats -> Math.round(floats[0]))
            .toArray();
        int[] federatedModelPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testX))
            .map(federatedModel::predict)
            .mapToInt(floats -> Math.round(floats[0]))
            .toArray();
        // 计算准确率
        double plainPredictAccuracy = Accuracy.of(truths, plainModelPredicts);
        double federatedPredictAccuracy = Accuracy.of(truths, federatedModelPredicts);
        LOGGER.info("ε = {}，plain acc. = {}，federated acc. = {}", epsilon, plainPredictAccuracy, federatedPredictAccuracy);
    }

    @Test
    public void testPlainTraining() {
        LOGGER.info("----{} plain training-----", name);
        XgBoostParams xgBoostParams = new XgBoostClsParams.Builder(OpBoostUtils.getNumClass(formula, train)).build();
        // 明文训练
        Predictor plainModel = plainTraining(name, formula, train, xgBoostParams);
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
        Predictor federatedModel = federateTraining(name, formula,
            hostDataFrame, hostConfig, leftSlaveDataFrame, leftSlaveConfig, rightSlaveDataFrame, rightSlaveConfig
        );
        // 明文训练验证预测结果完全一致性
        LOGGER.info("{} verify same prediction for train data", name);
        DataFrame trainX = formula.x(train);
        int[] plainPredictions = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainX))
            .map(plainModel::predict)
            .mapToInt(floats -> Math.round(floats[0]))
            .toArray();
        int[] federatedPredictions = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainX))
            .map(federatedModel::predict)
            .mapToInt(floats -> Math.round(floats[0]))
            .toArray();
        if (plainVerify) {
            // 明文训练验证预测结果完全一致性
            LOGGER.info("{} verify same prediction for train data", name);
            Assert.assertArrayEquals(plainPredictions, federatedPredictions);
        } else {
            // 特殊数据集统计预测结果一致的数量
            LOGGER.info("{} counting same prediction for train data", name);
            int totalNum = plainPredictions.length;
            long sameNum = IntStream.range(0, trainX.nrows())
                .filter(index -> plainPredictions[index] == federatedPredictions[index])
                .count();
            LOGGER.info("total_num = {}, same_num = {}", totalNum, sameNum);
        }
    }

    private Predictor plainTraining(String name, Formula formula, DataFrame data, XgBoostParams xgBoostParams) {
        try {
            LOGGER.info("-----{} plain training-----", name);
            Map<String, Object> params = xgBoostParams.getParams();
            int round = xgBoostParams.getTreeNum();
            DMatrix trainDataMatrix = OpXgBoostUtils.dataFrameToDataMatrix(formula, data);
            Booster booster = XGBoost.train(trainDataMatrix, params, round, new HashMap<>(), null, null);
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

    private Predictor federateTraining(String name, Formula formula,
                                       DataFrame hostDataFrame, OpXgBoostHostConfig hostConfig,
                                       DataFrame leftSlaveDataFrame, OpBoostSlaveConfig leftSlaveConfig,
                                       DataFrame rightSlaveDataFrame, OpBoostSlaveConfig rightSlaveConfig
                                       ) {
        LOGGER.info("-----{} training-----", name);
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
