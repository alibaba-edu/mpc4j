package edu.alibaba.mpc4j.sml.opboost.main.opboost;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlave;
import edu.alibaba.mpc4j.sml.opboost.OpBoostSlaveConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;

import java.util.concurrent.TimeUnit;

/**
 * OpBoost从机执行方。
 *
 * @author Weiran Liu
 * @date 2022/7/4
 */
public class OpBoostSlaveRunner implements OpBoostRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegOpBoostHostRunner.class);
    /**
     * 计时器
     */
    private final StopWatch stopWatch;
    /**
     * 从机
     */
    private final OpBoostSlave slave;
    /**
     * 从机通信接口。
     */
    private final Rpc slaveRpc;
    /**
     * 从机配置项
     */
    private final OpBoostSlaveConfig slaveConfig;
    /**
     * 总执行轮数
     */
    private final int totalRound;
    /**
     * 自己的数据帧
     */
    private final DataFrame ownDataFrame;
    /**
     * 总时间
     */
    private long totalTime;
    /**
     * 总数据包数量
     */
    private long totalPacketNum;
    /**
     * 总负载字节长度
     */
    private long totalPayloadByteLength;
    /**
     * 总发送字节长度
     */
    private long totalSendByteLength;

    public OpBoostSlaveRunner(OpBoostSlave slave, OpBoostSlaveConfig slaveConfig, int totalRound,
                              DataFrame ownDataFrame) {
        this.slave = slave;
        slaveRpc = slave.getRpc();
        this.slaveConfig = slaveConfig;
        stopWatch = new StopWatch();
        this.totalRound = totalRound;
        this.ownDataFrame = ownDataFrame;
    }


    @Override
    public void run() throws MpcAbortException {
        slaveRpc.synchronize();
        slaveRpc.reset();
        totalTime = 0L;
        totalPacketNum = 0L;
        totalPayloadByteLength = 0L;
        totalSendByteLength = 0L;
        // 重复实验，记录数据
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            slave.fit(ownDataFrame, slaveConfig);
            stopWatch.stop();
            // 记录时间
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            LOGGER.info("Round {}: Slave Time = {}ms", round, time);
            totalTime += time;
        }
        totalPacketNum = slaveRpc.getSendDataPacketNum();
        totalPayloadByteLength = slaveRpc.getPayloadByteLength();
        totalSendByteLength = slaveRpc.getSendByteLength();
        slaveRpc.reset();
    }

    @Override
    public double getTime() {
        return (double)totalTime / totalRound;
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
