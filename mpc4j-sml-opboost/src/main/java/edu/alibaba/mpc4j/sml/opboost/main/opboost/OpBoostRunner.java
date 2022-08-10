package edu.alibaba.mpc4j.sml.opboost.main.opboost;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * OpBoost执行方。
 *
 * @author Weiran Liu
 * @date 2022/7/4
 */
public interface OpBoostRunner {
    /**
     * 执行协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void run() throws MpcAbortException;

    /**
     * 返回（平均）执行时间。
     *
     * @return 执行时间。
     */
    double getTime();

    /**
     * 返回（平均）数据包数量。
     *
     * @return 数据包数量。
     */
    long getPacketNum();

    /**
     * 返回（平均）负载字节长度。
     *
     * @return 负载字节长度。
     */
    long getPayloadByteLength();

    /**
     * 返回（平均）发送字节长度。
     *
     * @return 发送字节长度。
     */
    long getSendByteLength();
}
