package edu.alibaba.mpc4j.sml.opboost.main.opboost;

import org.apache.commons.lang3.time.StopWatch;
import smile.data.DataFrame;
import smile.data.formula.Formula;

/**
 * 分类OpBoost执行方接口。
 *
 * @author Weiran Liu
 * @date 2022/7/8
 */
public abstract class AbstractOpBoostHostRunner implements OpBoostRunner {
    /**
     * 计时器
     */
    protected final StopWatch stopWatch;
    /**
     * 总执行轮数
     */
    protected final int totalRound;
    /**
     * 标签
     */
    protected final Formula formula;
    /**
     * 自己的数据帧
     */
    protected final DataFrame ownDataFrame;
    /**
     * 训练数据帧
     */
    protected final DataFrame trainFeatureDataFrame;
    /**
     * 测试数据帧
     */
    protected final DataFrame testFeatureDataFrame;
    /**
     * 总时间
     */
    protected long totalTime;
    /**
     * 训练总度量值
     */
    protected double totalTrainMeasure;
    /**
     * 测试总度量值
     */
    protected double totalTestMeasure;
    /**
     * 数据包数量
     */
    protected long totalPacketNum;
    /**
     * 负载字节长度
     */
    protected long totalPayloadByteLength;
    /**
     * 发送字节长度
     */
    protected long totalSendByteLength;

    public AbstractOpBoostHostRunner(int totalRound, Formula formula, DataFrame ownDataFrame,
                                     DataFrame trainFeatureDataFrame, DataFrame testFeatureDataFrame) {
        stopWatch = new StopWatch();
        this.totalRound = totalRound;
        this.formula = formula;
        this.ownDataFrame = ownDataFrame;
        this.trainFeatureDataFrame = trainFeatureDataFrame;
        this.testFeatureDataFrame = testFeatureDataFrame;
    }

    protected void reset() {
        totalTime = 0;
        totalTrainMeasure = 0;
        totalTestMeasure = 0;
        totalPacketNum = 0;
        totalPayloadByteLength = 0;
        totalSendByteLength = 0;
    }

    @Override
    public double getTime() {
        return (double)totalTime / totalRound;
    }

    /**
     * 返回训练度量值。
     *
     * @return 续联度量值。
     */
    public double getTrainMeasure() {
        return totalTrainMeasure / totalRound;
    }

    /**
     * 返回测试度量值。
     *
     * @return 测试度量值。
     */
    public double getTestMeasure() {
        return totalTestMeasure / totalRound;
    }

    @Override
    public long getPacketNum() {
        return totalPacketNum / totalRound;
    }

    @Override
    public long getPayloadByteLength() {
        return totalPayloadByteLength / totalRound;
    }

    @Override
    public long getSendByteLength() {
        return totalSendByteLength / totalRound;
    }
}
