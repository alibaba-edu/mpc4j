package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * DPPRF接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class DpprfReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final DpprfReceiver receiver;
    /**
     * α数组
     */
    private final int[] alphaArray;
    /**
     * α上界
     */
    private final int alphaBound;
    /**
     * 预计算接收方输出
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * 输出
     */
    private DpprfReceiverOutput receiverOutput;

    DpprfReceiverThread(DpprfReceiver receiver, int[] alphaArray, int alphaBound) {
        this(receiver, alphaArray, alphaBound, null);
    }

    DpprfReceiverThread(DpprfReceiver receiver, int[] alphaArray, int alphaBound, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alphaArray = alphaArray;
        this.alphaBound = alphaBound;
        this.preReceiverOutput = preReceiverOutput;
    }

    DpprfReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(alphaArray.length, alphaBound);
            receiverOutput = preReceiverOutput == null ? receiver.puncture(alphaArray, alphaBound)
                : receiver.puncture(alphaArray, alphaBound, preReceiverOutput);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
