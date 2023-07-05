package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

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
    private final HammingParty receiver;
    /**
     * x1
     */
    private final SquareZ2Vector x1;
    /**
     * 运算数量
     */
    private final int bitNum;
    /**
     * 汉明距离
     */
    private int hammingDistance;

    HammingReceiverThread(HammingParty receiver, SquareZ2Vector x1) {
        this.receiver = receiver;
        this.x1 = x1;
        bitNum = x1.getNum();
    }

    int getHammingDistance() {
        return hammingDistance;
    }

    @Override
    public void run() {
        try {
            receiver.init(bitNum);
            // 接收方先接收距离，再发送距离
            hammingDistance = receiver.receiveHammingDistance(x1);
            receiver.sendHammingDistance(x1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
