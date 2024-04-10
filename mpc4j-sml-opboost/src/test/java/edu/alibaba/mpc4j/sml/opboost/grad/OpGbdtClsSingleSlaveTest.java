package edu.alibaba.mpc4j.sml.opboost.grad;

import edu.alibaba.mpc4j.common.data.DataFrameUtils;
import edu.alibaba.mpc4j.common.data.DatasetManager;
import edu.alibaba.mpc4j.common.data.classification.BreastCancer;
import edu.alibaba.mpc4j.common.data.classification.Iris;
import edu.alibaba.mpc4j.common.data.classification.PenDigits;
import edu.alibaba.mpc4j.common.data.classification.Weather;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlave;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlaveConfig;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlaveThread;
import edu.alibaba.mpc4j.sml.opboost.OpBoostTestUtils;
import edu.alibaba.mpc4j.sml.smile.classification.GradientTreeBoost;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;

/**
 * 分类OpGradBoost单从机测试。
 *
 * @author Weiran Liu
 * @date 2021/09/26
 */
@RunWith(Parameterized.class)
public class OpGbdtClsSingleSlaveTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpGbdtClsSingleSlaveTest.class);

    static {
        DatasetManager.setPathPrefix("../data/");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // 两分类任务，特征均为枚举值
        configurations.add(new Object[]{"Weather", Weather.formula, Weather.data, Weather.data,});
        // 三分类任务，特征均为float
        configurations.add(new Object[]{"Iris", Iris.formula, Iris.data, Iris.data,});
        // 多分类任务，特征均为double
        configurations.add(new Object[]{"PenDigits", PenDigits.formula, PenDigits.data, PenDigits.data,});
        // 二分类任务，较大规模数据
        configurations.add(new Object[]{"BreastCancer", BreastCancer.formula, BreastCancer.data, BreastCancer.data});

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
    private final ClsOpGradBoostHost host;
    /**
     * 从机
     */
    private final OpBoostSlave slave;

    public OpGbdtClsSingleSlaveTest(String name, Formula formula, DataFrame train, DataFrame test) {
        super(name);
        this.name = name;
        this.formula = formula;
        this.train = train;
        this.test = test;
        host = new ClsOpGradBoostHost(firstRpc, secondRpc.ownParty());
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
        super.connect();
    }

    @Test
    public void testLargeLdpTraining() {
        testLdpTraining(OpBoostTestUtils.LARGE_EPSILON);
    }

    @Test
    public void testDefaultLdpTraining() {
        testLdpTraining(OpBoostTestUtils.DEFAULT_EPSILON);
    }

    @Test
    public void testSmallLdpTraining() {
        testLdpTraining(OpBoostTestUtils.SMALL_EPSILON);
    }

    private void testLdpTraining(double epsilon) {
        LOGGER.info("----{} LDP training, ε = {}-----", name, epsilon);
        // 明文训练
        GradientTreeBoost plainModel = plainTraining(formula, train);
        // 联邦训练
        DataFrame[] splitDataFrame = DataFrameUtils.split(formula.x(train), 2);
        DataFrame hostDataFrame = splitDataFrame[0].merge(formula.y(train));
        Map<String, LdpConfig> hostLdpConfigMap = OpBoostTestUtils.createLdpConfigMap(splitDataFrame[0], epsilon);
        ClsOpGradBoostHostConfig hostConfig = new ClsOpGradBoostHostConfig
            .Builder(hostDataFrame.schema())
            .addLdpConfig(hostLdpConfigMap)
            .build();
        DataFrame slaveDataFrame = splitDataFrame[1];
        Map<String, LdpConfig> slaveLdpConfigMap = OpBoostTestUtils.createLdpConfigMap(slaveDataFrame, epsilon);
        OpBoostSlaveConfig slaveConfig = new OpBoostSlaveConfig
            .Builder(slaveDataFrame.schema())
            .addLdpConfig(slaveLdpConfigMap)
            .build();
        GradientTreeBoost federatedModel = federateTraining(
             formula, hostDataFrame, hostConfig, slaveDataFrame, slaveConfig
        );
        LOGGER.info("{} verify difference between plain model and federated model", name);
        // 预测结果
        int[] truths = formula.y(test).toIntArray();
        int[] plainModelPredicts = plainModel.predict(test);
        int[] federatedModelPredicts = federatedModel.predict(test);
        // 计算准确率
        double plainPredictAccuracy = Accuracy.of(truths, plainModelPredicts);
        double federatedPredictAccuracy = Accuracy.of(truths, federatedModelPredicts);
        LOGGER.info("ε = {}，plain acc. = {}，federated acc. = {}", epsilon, plainPredictAccuracy, federatedPredictAccuracy);
    }

    @Test
    public void testPlainTraining() {
        LOGGER.info("----{} plain training-----", name);
        // 明文训练
        GradientTreeBoost plainModel = plainTraining(formula, train);
        // 联邦训练
        DataFrame[] splitDataFrame = DataFrameUtils.split(formula.x(train), 2);
        DataFrame hostDataFrame = splitDataFrame[0].merge(formula.y(train));
        ClsOpGradBoostHostConfig hostConfig = new ClsOpGradBoostHostConfig.Builder(hostDataFrame.schema()).build();
        DataFrame slaveDataFrame = splitDataFrame[1];
        OpBoostSlaveConfig slaveConfig = new OpBoostSlaveConfig.Builder(slaveDataFrame.schema()).build();
        GradientTreeBoost federatedModel = federateTraining(
            formula, hostDataFrame, hostConfig, slaveDataFrame, slaveConfig
        );
        LOGGER.info("{} verify same trained model", name);
        // 服务端模型应该和客户端模型一致，需要每颗回归树单独对比
        Assert.assertEquals(plainModel.trees().length, federatedModel.trees().length);
        IntStream.range(0, plainModel.trees().length).forEach(treeIndex ->
            Assert.assertEquals(
                plainModel.trees()[treeIndex].toString(), federatedModel.trees()[treeIndex].toString()
            )
        );
    }

    private GradientTreeBoost plainTraining(Formula formula, DataFrame data) {
        Properties plainSmileProperties = new ClsOpGradBoostHostConfig.Builder(data.schema()).build().getSmileProperties();
        return GradientTreeBoost.fit(formula, data, plainSmileProperties);
    }

    private GradientTreeBoost federateTraining(Formula formula,
                                               DataFrame hostDataFrame, ClsOpGradBoostHostConfig hostConfig,
                                               DataFrame slaveDataFrame, OpBoostSlaveConfig slaveConfig) {
        int randomTaskId = Math.abs(OpBoostTestUtils.SECURE_RANDOM.nextInt());
        host.setTaskId(randomTaskId);
        slave.setTaskId(randomTaskId);
        try {
            OpGbdtClsHostThread host2PcThread = new OpGbdtClsHostThread(host, formula, hostDataFrame, hostConfig);
            OpBoostSlaveThread slave2PcThread = new OpBoostSlaveThread(slave, slaveDataFrame, slaveConfig);
            // 开始执行协议
            host2PcThread.start();
            slave2PcThread.start();
            // 等待线程停止
            host2PcThread.join();
            slave2PcThread.join();
            // 返回模型
            return host2PcThread.getModel();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Error for " + name);
        }
    }
}
