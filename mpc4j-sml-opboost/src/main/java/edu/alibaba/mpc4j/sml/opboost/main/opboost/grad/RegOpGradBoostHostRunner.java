package edu.alibaba.mpc4j.sml.opboost.main.opboost.grad;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.sml.opboost.grad.RegOpGradBoostHost;
import edu.alibaba.mpc4j.sml.opboost.grad.RegOpGradBoostHostConfig;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.AbstractOpBoostHostRunner;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.RegOpBoostHostRunner;
import edu.alibaba.mpc4j.sml.smile.regression.GradientTreeBoost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.validation.metric.MSE;

import java.util.concurrent.TimeUnit;

/**
 * 回归OpGardBoost执行方。
 *
 * @author Weiran Liu
 * @date 2022/7/4
 */
class RegOpGradBoostHostRunner extends AbstractOpBoostHostRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegOpBoostHostRunner.class);
    /**
     * 主机
     */
    private final RegOpGradBoostHost host;
    /**
     * 主机通信接口。
     */
    private final Rpc hostRpc;
    /**
     * 主机配置项
     */
    private final RegOpGradBoostHostConfig hostConfig;
    /**
     * 训练真实值
     */
    private final double[] trainTruths;
    /**
     * 测试真实值
     */
    private final double[] testTruths;

    RegOpGradBoostHostRunner(RegOpGradBoostHost host, RegOpGradBoostHostConfig hostConfig, int totalRound,
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
        // 重复实验，记录数据
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            GradientTreeBoost model = host.fit(formula, ownDataFrame, hostConfig);
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
        totalPacketNum = hostRpc.getSendDataPacketNum();
        totalPayloadByteLength = hostRpc.getPayloadByteLength();
        totalSendByteLength = hostRpc.getSendByteLength();
        hostRpc.reset();
    }
}
