package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiverOutput;

/**
 * BSP-COT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class BspCotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final BspCotReceiver receiver;
    /**
     * α数组
     */
    private final int[] alphaArray;
    /**
     * 数量
     */
    private final int num;
    /**
     * 预计算接收方输出
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * 输出
     */
    private BspCotReceiverOutput receiverOutput;

    BspCotReceiverThread(BspCotReceiver receiver, int[] alphaArray, int num) {
        this(receiver, alphaArray, num, null);
    }

    BspCotReceiverThread(BspCotReceiver receiver, int[] alphaArray, int num, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alphaArray = alphaArray;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    BspCotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(alphaArray.length, num);
            receiverOutput = preReceiverOutput == null ?
                receiver.receive(alphaArray, num) : receiver.receive(alphaArray, num, preReceiverOutput);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
