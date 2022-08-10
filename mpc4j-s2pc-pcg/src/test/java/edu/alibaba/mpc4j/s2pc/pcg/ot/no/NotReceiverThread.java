package edu.alibaba.mpc4j.s2pc.pcg.ot.no;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * n选1-OT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
class NotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final NotReceiver receiver;
    /**
     * 最大选择值
     */
    private final int n;
    /**
     * 选择值数组
     */
    private final int[] choices;
    /**
     * 输出
     */
    private NotReceiverOutput receiverOutput;

    NotReceiverThread(NotReceiver receiver, int n, int[] choices) {
        this.receiver = receiver;
        this.n = n;
        this.choices = choices;
    }

    NotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(n, choices.length);
            receiverOutput = receiver.receive(choices);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
