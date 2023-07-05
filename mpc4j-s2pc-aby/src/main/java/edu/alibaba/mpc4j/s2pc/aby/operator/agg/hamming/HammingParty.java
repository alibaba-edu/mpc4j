package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * 汉明距离协议服务端接口。
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
public interface HammingParty extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxBitNum 最大比特数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBitNum) throws MpcAbortException;

    /**
     * 计算x0和x1的汉明距离，不接收结果。
     *
     * @param xi xi，服务端为x0，客户端为x1。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void sendHammingDistance(SquareZ2Vector xi) throws MpcAbortException;

    /**
     * 计算x0和x1的汉明距离，并接收结果。
     *
     * @param xi xi，服务端为x0，客户端为x1.
     * @return x0和x1的汉明距离。
     * @throws MpcAbortException 如果协议异常中止。
     */
    int receiveHammingDistance(SquareZ2Vector xi) throws MpcAbortException;
}
