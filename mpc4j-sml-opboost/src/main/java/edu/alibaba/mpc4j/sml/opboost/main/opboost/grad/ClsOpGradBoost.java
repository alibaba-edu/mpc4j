package edu.alibaba.mpc4j.sml.opboost.main.opboost.grad;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.grad.ClsOpGradBoostHost;
import edu.alibaba.mpc4j.sml.opboost.grad.ClsOpGradBoostHostConfig;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostLdpType;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostTaskType;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.AbstractClsOpBoost;
import edu.alibaba.mpc4j.sml.smile.classification.GradientTreeBoost;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.Tuple;
import smile.validation.metric.AUC;
import smile.validation.metric.Accuracy;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 明文分类OpGradBoost。
 *
 * @author Weiran Liu
 * @date 2022/7/2
 */
public class ClsOpGradBoost extends AbstractClsOpBoost {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClsOpGradBoost.class);

    public ClsOpGradBoost(Properties properties) {
        super(properties, OpBoostTaskType.CLS_OP_GRAD_BOOST);
    }

    @Override
    public void run() throws IOException, MpcAbortException {
        String filePath = taskType
            // 数据集名称
            + "_" + datasetName
            // 测试轮数
            + "_" + totalRound
            // 参与方ID
            + "_" + ownRpc.ownParty().getPartyId()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 输出表头
        String tab = "name\tε\tθ\tα\tTime(ms)\t" +
            "Train Measure\tTest Measure\t" +
            "Send Packet Num\tSend Payload Bytes(B)\tSend Total Bytes(B)";
        printWriter.println(tab);
        // 创建链接
        ownRpc.connect();
        if (ownRpc.ownParty().getPartyId() == 0) {
            // 明文训练
            runHostPlainTraining(printWriter);
            // 分段
            runHostEpsilonLdpTraining(printWriter, OpBoostLdpType.PIECEWISE);
            // GlobalMap
            runHostEpsilonLdpTraining(printWriter, OpBoostLdpType.GLOBAL_MAP);
            // GlobalExpMap
            runHostEpsilonLdpTraining(printWriter, OpBoostLdpType.GLOBAL_EXP_MAP);
            // LocalMap
            runHostLocalMapLdpTraining(printWriter, OpBoostLdpType.LOCAL_MAP);
            // LocalExpMap
            runHostLocalMapLdpTraining(printWriter, OpBoostLdpType.LOCAL_EXP_MAP);
            // AdjMap
            runHostAdjMapLdpTraining(printWriter, OpBoostLdpType.ADJ_MAP);
            // AdjExpMap
            runHostAdjMapLdpTraining(printWriter, OpBoostLdpType.ADJ_EXP_MAP);
        } else {
            // 明文训练
            runSlavePlainTraining(printWriter);
            // 分段
            runSlaveEpsilonLdpTraining(printWriter, OpBoostLdpType.PIECEWISE);
            // GlobalMap
            runSlaveEpsilonLdpTraining(printWriter, OpBoostLdpType.GLOBAL_MAP);
            // GlobalExpMap
            runSlaveEpsilonLdpTraining(printWriter, OpBoostLdpType.GLOBAL_EXP_MAP);
            // LocalMap
            runSlaveLocalMapLdpTraining(printWriter, OpBoostLdpType.LOCAL_MAP);
            // LocalExpMap
            runSlaveLocalMapLdpTraining(printWriter, OpBoostLdpType.LOCAL_EXP_MAP);
            // AdjMap
            runSlaveAdjMapLdpTraining(printWriter, OpBoostLdpType.ADJ_MAP);
            // AdjExpMap
            runSlaveAdjMapLdpTraining(printWriter, OpBoostLdpType.ADJ_EXP_MAP);
        }
        // 清理状态
        printWriter.close();
        fileWriter.close();
        ownRpc.disconnect();
    }

    private void runHostPlainTraining(PrintWriter printWriter) {
        // 主机做明文训练
        LOGGER.info("-----{} for {}-----", OpBoostLdpType.PLAIN, taskType);
        double totalTrainMeasure = 0.0;
        double totalTestMeasure = 0.0;
        long totalTime = 0L;
        StopWatch stopWatch = new StopWatch();
        Properties smileProperties = new ClsOpGradBoostHostConfig.Builder(trainDataFrame.schema())
            .setTreeNum(treeNum)
            .setMaxDepth(maxDepth)
            .setShrinkage(shrinkage)
            .build()
            .getSmileProperties();
        // 预热
        GradientTreeBoost.fit(formula, trainDataFrame, smileProperties);
        // 重复实验，记录数据
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            GradientTreeBoost model = GradientTreeBoost.fit(formula, trainDataFrame, smileProperties);
            stopWatch.stop();
            // 记录时间
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            double trainMeasure;
            double testMeasure;
            if (numClass == 2) {
                // 如果是二分类问题，则计算AUC
                double[][] trainProbabilities = new double[trainFeatureDataFrame.nrows()][numClass];
                Tuple[] trainTuples = trainFeatureDataFrame.stream().toArray(Tuple[]::new);
                model.predict(trainTuples, trainProbabilities);
                double[] trainAucProbabilities = Arrays.stream(trainProbabilities)
                    .mapToDouble(probability -> probability[1])
                    .toArray();
                trainMeasure = AUC.of(trainTruths, trainAucProbabilities);
                double[][] testProbabilities = new double[testDataFrame.nrows()][numClass];
                Tuple[] testTuples = testFeatureDataFrame.stream().toArray(Tuple[]::new);
                model.predict(testTuples, testProbabilities);
                double[] testAucProbabilities = Arrays.stream(testProbabilities)
                    .mapToDouble(probability -> probability[1])
                    .toArray();
                testMeasure = AUC.of(testTruths, testAucProbabilities);
                LOGGER.info("Round {}: Time = {}ms, Train AUC = {}, Test AUC = {}",
                    round, time, trainMeasure, testMeasure
                );
            } else {
                // 如果是多分类问题，则计算准确率
                int[] trainPredicts = model.predict(trainFeatureDataFrame);
                trainMeasure = Accuracy.of(trainTruths, trainPredicts);
                int[] testPredicts = model.predict(testFeatureDataFrame);
                testMeasure = Accuracy.of(testTruths, testPredicts);
                LOGGER.info("Round {}: Time = {}ms, Train Acc. = {}, Test Acc. = {}",
                    round, time, trainMeasure, testMeasure
                );
            }
            totalTrainMeasure += trainMeasure;
            totalTestMeasure += testMeasure;
            totalTime += time;
        }
        double time = (double) totalTime / totalRound;
        double trainMeasure = totalTrainMeasure / totalRound;
        double testMeasure = totalTestMeasure / totalRound;
        long sendDataPacketNum = ownRpc.getSendDataPacketNum() / totalRound;
        long payloadByteLength = ownRpc.getPayloadByteLength() / totalRound;
        long sendByteLength = ownRpc.getSendByteLength() / totalRound;
        writeInfo(printWriter, OpBoostLdpType.PLAIN.name(), null, null, null, time,
            trainMeasure, testMeasure,
            sendDataPacketNum, payloadByteLength, sendByteLength
        );
        ownRpc.reset();
    }

    private ClsOpGradBoostHostConfig createHostConfig(Map<String, LdpConfig> ldpConfigs) {
        return new ClsOpGradBoostHostConfig.Builder(ownSchema)
            .setMaxDepth(maxDepth)
            .setTreeNum(treeNum)
            .setShrinkage(shrinkage)
            .addLdpConfig(ldpConfigs)
            .build();
    }

    private void runHostEpsilonLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType) throws MpcAbortException {
        for (double epsilon : epsilons) {
            LOGGER.info("-----{} for {}: ε = {}-----", ldpType.name(), taskType, epsilon);
            Map<String, LdpConfig> ldpConfigs = createLdpConfigs(ldpType, epsilon);
            ClsOpGradBoostHost host = new ClsOpGradBoostHost(ownRpc, otherParty);
            ClsOpGradBoostHostConfig hostConfig = createHostConfig(ldpConfigs);
            ClsOpGradBoostHostRunner hostRunner = new ClsOpGradBoostHostRunner(
                host, hostConfig, totalRound, formula, numClass, ownDataFrame,
                trainFeatureDataFrame, trainTruths, testFeatureDataFrame, testTruths
            );
            host.init();
            hostRunner.run();
            host.destroy();
            writeInfo(printWriter, ldpType.name(), epsilon, null, null, hostRunner.getTime(),
                hostRunner.getTrainMeasure(), hostRunner.getTestMeasure(),
                hostRunner.getPacketNum(), hostRunner.getPayloadByteLength(), hostRunner.getSendByteLength()
            );
        }
    }

    private void runHostLocalMapLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType) throws MpcAbortException {
        for (int theta : thetas) {
            for (double epsilon : epsilons) {
                LOGGER.info("-----{} for {}: ε = {}, θ = {}-----", ldpType.name(), taskType, epsilon, theta);
                Map<String, LdpConfig> ldpConfigs = createLdpConfigs(ldpType, epsilon, theta);
                ClsOpGradBoostHost host = new ClsOpGradBoostHost(ownRpc, otherParty);
                ClsOpGradBoostHostConfig hostConfig = createHostConfig(ldpConfigs);
                ClsOpGradBoostHostRunner hostRunner = new ClsOpGradBoostHostRunner(
                    host, hostConfig, totalRound, formula, numClass, ownDataFrame,
                    trainFeatureDataFrame, trainTruths, testFeatureDataFrame, testTruths
                );
                host.init();
                hostRunner.run();
                host.destroy();
                writeInfo(printWriter, ldpType.name(), epsilon, theta, null, hostRunner.getTime(),
                    hostRunner.getTrainMeasure(), hostRunner.getTestMeasure(),
                    hostRunner.getPacketNum(), hostRunner.getPayloadByteLength(), hostRunner.getSendByteLength()
                );
            }
        }
    }

    private void runHostAdjMapLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType) throws MpcAbortException {
        LOGGER.info("-----Host {} LDP training for {}-----", ldpType.name(), taskType);
        for (double alpha : alphas) {
            for (int theta : thetas) {
                for (double epsilon : epsilons) {
                    LOGGER.info("-----{} for {}: ε = {}, θ = {}, α = {}-----", ldpType.name(), taskType, epsilon, theta, alpha);
                    Map<String, LdpConfig> ldpConfigs = createLdpConfigs(ldpType, epsilon, theta, alpha);
                    ClsOpGradBoostHost host = new ClsOpGradBoostHost(ownRpc, otherParty);
                    ClsOpGradBoostHostConfig hostConfig = createHostConfig(ldpConfigs);
                    ClsOpGradBoostHostRunner hostRunner = new ClsOpGradBoostHostRunner(
                        host, hostConfig, totalRound, formula, numClass, ownDataFrame,
                        trainFeatureDataFrame, trainTruths, testFeatureDataFrame, testTruths
                    );
                    host.init();
                    hostRunner.run();
                    host.destroy();
                    writeInfo(printWriter, ldpType.name(), epsilon, theta, alpha, hostRunner.getTime(),
                        hostRunner.getTrainMeasure(), hostRunner.getTestMeasure(),
                        hostRunner.getPacketNum(),
                        hostRunner.getPayloadByteLength(), hostRunner.getSendByteLength()
                    );
                }
            }
        }
    }
}
