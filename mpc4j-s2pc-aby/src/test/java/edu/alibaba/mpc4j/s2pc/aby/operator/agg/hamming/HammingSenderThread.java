package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * 汉明距离协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
class HammingSenderThread extends Thread {
    /**
     * 汉明距离协议发送方
     */
    private final HammingParty sender;
    /**
     * xi
     */
    private final SquareZ2Vector x0;
    /**
     * 运算数量
     */
    private final int bitNum;
    /**
     * 汉明距离
     */
    private int hammingDistance;

    HammingSenderThread(HammingParty sender, SquareZ2Vector x0) {
        this.sender = sender;
        this.x0 = x0;
        bitNum = x0.getNum();
    }

    int getHammingDistance() {
        return hammingDistance;
    }

    @Override
    public void run() {
        try {
            sender.init(bitNum);
            // 发送方先发送距离，再接收距离
            sender.sendHammingDistance(x0);
            hammingDistance = sender.receiveHammingDistance(x0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
