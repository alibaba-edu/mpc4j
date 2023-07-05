package edu.alibaba.mpc4j.sml.opboost.main.opboost.grad;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.grad.RegOpGradBoostHost;
import edu.alibaba.mpc4j.sml.opboost.grad.RegOpGradBoostHostConfig;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostLdpType;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostTaskType;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.AbstractRegOpBoost;
import edu.alibaba.mpc4j.sml.smile.regression.GradientTreeBoost;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.validation.metric.MSE;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 明文回归OpGradBoost。
 *
 * @author Weiran Liu
 * @date 2022/7/1
 */
public class RegOpGradBoost extends AbstractRegOpBoost {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegOpGradBoost.class);


    public RegOpGradBoost(Properties properties) {
        super(properties, OpBoostTaskType.REG_OP_GRAD_BOOST);
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
        LOGGER.info("-----{} for {}-----", OpBoostLdpType.PLAIN.name(), taskType);
        StopWatch stopWatch = new StopWatch();
        double totalTrainMeasure = 0.0;
        double totalTestMeasure = 0.0;
        long totalTime = 0L;
        Properties smileProperties = new RegOpGradBoostHostConfig.Builder(trainDataFrame.schema())
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
            // 记录MSE
            double[] trainPredicts = model.predict(trainFeatureDataFrame);
            double trainMeasure = MSE.of(trainTruths, trainPredicts);
            double[] testPredicts = model.predict(testFeatureDataFrame);
            double testMeasure = MSE.of(testTruths, testPredicts);
            LOGGER.info("Round {}: Time = {}ms, Train MSE = {}, Test MSE = {}", round, time, trainMeasure, testMeasure);
            totalTrainMeasure += trainMeasure;
            totalTestMeasure += testMeasure;
            totalTime += time;
        }
        double time = (double)totalTime / totalRound;
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

    private RegOpGradBoostHostConfig createHostConfig(Map<String, LdpConfig> ldpConfigs) {
        return new RegOpGradBoostHostConfig.Builder(ownSchema)
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
            RegOpGradBoostHost host = new RegOpGradBoostHost(ownRpc, otherParty);
            RegOpGradBoostHostConfig hostConfig = createHostConfig(ldpConfigs);
            RegOpGradBoostHostRunner hostRunner = new RegOpGradBoostHostRunner(
                host, hostConfig, totalRound, formula, ownDataFrame,
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
                RegOpGradBoostHost host = new RegOpGradBoostHost(ownRpc, otherParty);
                RegOpGradBoostHostConfig hostConfig = createHostConfig(ldpConfigs);
                RegOpGradBoostHostRunner hostRunner = new RegOpGradBoostHostRunner(
                    host, hostConfig, totalRound, formula, ownDataFrame,
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
        for (double alpha : alphas) {
            for (int theta : thetas) {
                for (double epsilon : epsilons) {
                    LOGGER.info("-----{} for {}: ε = {}, θ = {}, α = {}-----", ldpType.name(), taskType, epsilon, theta, alpha);
                    Map<String, LdpConfig> ldpConfigs = createLdpConfigs(ldpType, epsilon, theta, alpha);
                    RegOpGradBoostHost host = new RegOpGradBoostHost(ownRpc, otherParty);
                    RegOpGradBoostHostConfig hostConfig = createHostConfig(ldpConfigs);
                    RegOpGradBoostHostRunner hostRunner = new RegOpGradBoostHostRunner(
                        host, hostConfig, totalRound, formula, ownDataFrame,
                        trainFeatureDataFrame, trainTruths, testFeatureDataFrame, testTruths
                    );
                    host.init();
                    hostRunner.run();
                    host.destroy();
                    writeInfo(printWriter, ldpType.name(), epsilon, theta, alpha, hostRunner.getTime(),
                        hostRunner.getTrainMeasure(), hostRunner.getTestMeasure(),
                        hostRunner.getPacketNum(), hostRunner.getPayloadByteLength(), hostRunner.getSendByteLength()
                    );
                }
            }
        }
    }
}
