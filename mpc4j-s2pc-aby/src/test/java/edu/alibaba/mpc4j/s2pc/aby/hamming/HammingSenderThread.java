package edu.alibaba.mpc4j.s2pc.aby.hamming;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

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
    private final HammingParty hammingSender;
    /**
     * xi
     */
    private final SquareSbitVector x0;
    /**
     * 运算数量
     */
    private final int bitNum;
    /**
     * 汉明距离
     */
    private int hammingDistance;

    HammingSenderThread(HammingParty hammingSender, SquareSbitVector x0) {
        this.hammingSender = hammingSender;
        this.x0 = x0;
        bitNum = x0.bitNum();
    }

    int getHammingDistance() {
        return hammingDistance;
    }

    @Override
    public void run() {
        try {
            hammingSender.getRpc().connect();
            hammingSender.init(bitNum);
            // 发送方先发送距离，再接收距离
            hammingSender.sendHammingDistance(x0);
            hammingDistance = hammingSender.receiveHammingDistance(x0);
            hammingSender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
