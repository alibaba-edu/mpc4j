package edu.alibaba.mpc4j.s2pc.aby.hamming;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

/**
 * 汉明距离协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
class HammingReceiverThread extends Thread {
    /**
     * 汉明距离协议接收方
     */
    private final HammingParty hammingReceiver;
    /**
     * x1
     */
    private final SquareSbitVector x1;
    /**
     * 运算数量
     */
    private final int bitNum;
    /**
     * 汉明距离
     */
    private int hammingDistance;

    HammingReceiverThread(HammingParty hammingReceiver, SquareSbitVector x1) {
        this.hammingReceiver = hammingReceiver;
        this.x1 = x1;
        bitNum = x1.bitNum();
    }

    int getHammingDistance() {
        return hammingDistance;
    }

    @Override
    public void run() {
        try {
            hammingReceiver.getRpc().connect();
            hammingReceiver.init(bitNum);
            // 接收方先接收距离，再发送距离
            hammingDistance = hammingReceiver.receiveHammingDistance(x1);
            hammingReceiver.sendHammingDistance(x1);
            hammingReceiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
