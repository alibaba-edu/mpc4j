package edu.alibaba.mpc4j.sml.opboost.main.opboost.xgboost;

import biz.k11i.xgboost.Predictor;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.AbstractOpBoostHostRunner;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.RegOpBoostHostRunner;
import edu.alibaba.mpc4j.sml.opboost.xgboost.OpXgBoostHost;
import edu.alibaba.mpc4j.sml.opboost.xgboost.OpXgBoostHostConfig;
import edu.alibaba.mpc4j.sml.opboost.xgboost.OpXgBoostUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.validation.metric.MSE;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 回归OpXgBoost执行方。
 *
 * @author Weiran Liu
 * @date 2022/7/5
 */
class RegOpXgBoostHostRunner extends AbstractOpBoostHostRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegOpBoostHostRunner.class);
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
     * 训练真实值
     */
    private final double[] trainTruths;
    /**
     * 测试真实值
     */
    private final double[] testTruths;

    RegOpXgBoostHostRunner(OpXgBoostHost host, OpXgBoostHostConfig hostConfig, int totalRound,
                           Formula formula, DataFrame ownDataFrame,
                           DataFrame trainFeatureDataFrame, double[] trainTruths,
                           DataFrame testFeatureDataFrame, double[] testTruths) {
        super(totalRound, formula, ownDataFrame, trainFeatureDataFrame, testFeatureDataFrame);
        this.host = host;
        hostRpc = host.getRpc();
        this.hostConfig = hostConfig;
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
            // 记录准确率
            double[] trainPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(trainFeatureDataFrame))
                .parallel()
                .map(model::predict)
                .mapToDouble(floats -> floats[0])
                .toArray();
            double trainMse = MSE.of(trainTruths, trainPredicts);
            double[] testPredicts = Arrays.stream(OpXgBoostUtils.dataFrameToFeatureVector(testFeatureDataFrame))
                .parallel()
                .map(model::predict)
                .mapToDouble(floats -> floats[0])
                .toArray();
            double testMse = MSE.of(testTruths, testPredicts);
            LOGGER.info("Round {}: Time = {}ms, Train MSE = {}, Test MSE = {}", round, time, trainMse, testMse);
            totalTrainMeasure += trainMse;
            totalTestMeasure += testMse;
            totalTime += time;
        }
        totalPacketNum = hostRpc.getSendDataPacketNum();
        totalPayloadByteLength = hostRpc.getPayloadByteLength();
        totalSendByteLength = hostRpc.getSendByteLength();
        hostRpc.reset();
    }
}
