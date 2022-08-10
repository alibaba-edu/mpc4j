package edu.alibaba.mpc4j.sml.opboost.main.opboost.xgboost;

import biz.k11i.xgboost.Predictor;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.AbstractOpBoostHostRunner;
import edu.alibaba.mpc4j.sml.opboost.xgboost.OpXgBoostHost;
import edu.alibaba.mpc4j.sml.opboost.xgboost.OpXgBoostHostConfig;
import edu.alibaba.mpc4j.sml.opboost.xgboost.OpXgBoostUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.validation.metric.AUC;
import smile.validation.metric.Accuracy;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 回归OpXgBoost执行方。
 *
 * @author Weiran Liu
 * @date 2022/7/5
 */
class ClsOpXgBoostHostRunner extends AbstractOpBoostHostRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClsOpXgBoostHostRunner.class);
    /**
     * 主机
     */
    private final OpXgBoostHost host;
    /**
     * 主机通信接口。
     */
    private final Rpc hostRpc;
    /**
     * 主机配置项
     */
    private final OpXgBoostHostConfig hostConfig;
    /**
     * 分类数量
     */
    private final int numClass;
    /**
     * 训练真实值
     */
    private final int[] trainTruths;
    /**
     * 测试真实值
     */
    private final int[] testTruths;

    ClsOpXgBoostHostRunner(OpXgBoostHost host, OpXgBoostHostConfig hostConfig, int totalRound,
                           Formula formula, int numClass, DataFrame ownDataFrame,
                           DataFrame trainFeatureDataFrame, int[] trainTruths,
                           DataFrame testFeatureDataFrame, int[] testTruths) {
        super(totalRound, formula, ownDataFrame, trainFeatureDataFrame, testFeatureDataFrame);
        this.host = host;
        hostRpc = host.getRpc();
        this.hostConfig = hostConfig;
        this.numClass = numClass;
        this.trainTruths = trainTruths;
        this.testTruths = testTruths;
    }

    @Override
    public void run() throws MpcAbortException {
        hostRpc.synchronize();
        hostRpc.reset();
        reset();
        // 重复实验，记录数据
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            Predictor model = host.fit(formula, ownDataFrame, hostConfig);
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
        totalPacketNum = hostRpc.getSendDataPacketNum();
        totalPayloadByteLength = hostRpc.getPayloadByteLength();
        totalSendByteLength = hostRpc.getSendByteLength();
        hostRpc.reset();
    }
}
