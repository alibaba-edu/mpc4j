package edu.alibaba.mpc4j.sml.opboost;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import smile.data.DataFrame;

/**
 * OpBoost从机线程。
 *
 * @author Weiran Liu
 * @date 2021/10/08
 */
public class OpBoostSlaveThread extends Thread {
    /**
     * 从机
     */
    private final OpBoostSlave slave;
    /**
     * 从机训练数据
     */
    private final DataFrame slaveDataFrame;
    /**
     * 从机配置参数
     */
    private final OpBoostSlaveConfig slaveConfig;

    public OpBoostSlaveThread(OpBoostSlave slave, DataFrame slaveDataFrame, OpBoostSlaveConfig slaveConfig) {
        this.slave = slave;
        this.slaveDataFrame = slaveDataFrame;
        this.slaveConfig = slaveConfig;
    }

    @Override
    public void run() {
        try {
            slave.fit(slaveDataFrame, slaveConfig);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
