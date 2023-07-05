package edu.alibaba.mpc4j.sml.opboost.main.opboost.xgboost;

import biz.k11i.xgboost.Predictor;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostLdpType;
import edu.alibaba.mpc4j.sml.opboost.main.OpBoostTaskType;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.AbstractClsOpBoost;
import edu.alibaba.mpc4j.sml.opboost.xgboost.*;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.validation.metric.AUC;
import smile.validation.metric.Accuracy;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 分类OpXgBoost。
 *
 * @author Weiran Liu
 * @date 2022/7/2
 */
public class ClsOpXgBoost extends AbstractClsOpBoost {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClsOpXgBoost.class);

    public ClsOpXgBoost(Properties properties) {
        super(properties, OpBoostTaskType.CLS_OP_XG_BOOST);
    }

    @Override
    public void run() throws IOException, XGBoostError, MpcAbortException {
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

    private void runHostPlainTraining(PrintWriter printWriter) throws XGBoostError, IOException {
        LOGGER.info("-----{} for {}-----", OpBoostLdpType.PLAIN.name(), taskType);
        StopWatch stopWatch = new StopWatch();
        double totalTrainMeasure = 0.0;
        double totalTestMeasure = 0.0;
        long totalTime = 0L;
        XgBoostParams xgBoostParams = new XgBoostClsParams.Builder(numClass)
            .setTreeNum(treeNum)
            .setMaxDepth(maxDepth)
            .setShrinkage(shrinkage)
            .build();
        Map<String, Object> params = xgBoostParams.getParams();
        DMatrix trainDataMatrix = OpXgBoostUtils.dataFrameToDataMatrix(formula, trainDataFrame);
        // 预热
        XGBoost.train(trainDataMatrix, params, treeNum, new HashMap<>(0), null, null);
        // 重复实验，记录数据
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            Booster booster = XGBoost.train(trainDataMatrix, params, treeNum, new HashMap<>(0), null, null);
            String modelName = taskType + "_" + round + ".deprecated";
            booster.saveModel(modelName);
            File modelFile = new File(modelName);
            FileInputStream fileInputStream = new FileInputStream(modelFile);
            Predictor model = new Predictor(fileInputStream);
            fileInputStream.close();
            modelFile.deleteOnExit();
            stopWatch.stop();
            // 记录时间
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            double trainMeasure;
            double testMeasure;
            if (numClass == 2) {
                // 如果是二分类，则记录AUC
                double[] trainProbabilities = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainFeatureDataFrame))
                    .parallel()
                    .map(model::predict)
                    .mapToDouble(floats -> floats[0])
                    .toArray();
                trainMeasure = AUC.of(trainTruths, trainProbabilities);
                double[] testProbabilities = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testFeatureDataFrame))
                    .parallel()
                    .map(model::predict)
                    .mapToDouble(floats -> floats[0])
                    .toArray();
                testMeasure = AUC.of(testTruths, testProbabilities);
                LOGGER.info("Round {}: Time = {}ms, Train AUC = {}, Test AUC = {}",
                    round, time, trainMeasure, testMeasure
                );
            } else {
                // 如果是多分类问题，则计算准确率
                int[] trainPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainFeatureDataFrame))
                    .parallel()
                    .map(model::predict)
                    .mapToInt(floats -> Math.round(floats[0]))
                    .toArray();
                trainMeasure = Accuracy.of(trainTruths, trainPredicts);
                int[] testPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testFeatureDataFrame))
                    .parallel()
                    .map(model::predict)
                    .mapToInt(floats -> Math.round(floats[0]))
                    .toArray();
                testMeasure = Accuracy.of(testTruths, testPredicts);
                LOGGER.info("Round {}: Time = {}ms, Train Acc. = {}, Test Acc. = {}",
                    round, time, trainMeasure, testMeasure
                );
            }
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

    private OpXgBoostHostConfig createHostConfig(Map<String, LdpConfig> ldpConfigs) {
        XgBoostParams xgBoostParams = new XgBoostClsParams.Builder(numClass)
            .setMaxDepth(maxDepth)
            .setTreeNum(treeNum)
            .setShrinkage(shrinkage)
            .build();
        return new OpXgBoostHostConfig
            .Builder(ownSchema, xgBoostParams)
            .addLdpConfig(ldpConfigs)
            .build();
    }

    private void runHostEpsilonLdpTraining(PrintWriter printWriter, OpBoostLdpType ldpType) throws MpcAbortException {
        for (double epsilon : epsilons) {
            LOGGER.info("-----{} for {}: ε = {}-----", ldpType.name(), taskType, epsilon);
            Map<String, LdpConfig> ldpConfigs = createLdpConfigs(ldpType, epsilon);
            OpXgBoostHost host = new OpXgBoostHost(ownRpc, otherParty);
            OpXgBoostHostConfig hostConfig = createHostConfig(ldpConfigs);
            ClsOpXgBoostHostRunner hostRunner = new ClsOpXgBoostHostRunner(
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
                OpXgBoostHost host = new OpXgBoostHost(ownRpc, otherParty);
                OpXgBoostHostConfig hostConfig = createHostConfig(ldpConfigs);
                ClsOpXgBoostHostRunner hostRunner = new ClsOpXgBoostHostRunner(
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
                    OpXgBoostHost host = new OpXgBoostHost(ownRpc, otherParty);
                    OpXgBoostHostConfig hostConfig = createHostConfig(ldpConfigs);
                    ClsOpXgBoostHostRunner hostRunner = new ClsOpXgBoostHostRunner(
                        host, hostConfig, totalRound, formula, numClass, ownDataFrame,
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
