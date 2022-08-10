package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 基础N选1-OT协议接收方线程。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
class BnotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final BnotReceiver receiver;
    /**
     * 选择数量
     */
    private final int n;
    /**
     * 选择数组
     */
    private final int[] choices;
    /**
     * 输出
     */
    private BnotReceiverOutput receiverOutput;

    BnotReceiverThread(BnotReceiver receiver, int[] choices, int n) {
        this.receiver = receiver;
        this.choices = choices;
        this.n = n;
    }

    BnotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(n);
            receiverOutput = receiver.receive(choices);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
