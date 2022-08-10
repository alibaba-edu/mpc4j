package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * COT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final CotReceiver receiver;
    /**
     * 选择比特
     */
    private final boolean[] choices;
    /**
     * 输出
     */
    private CotReceiverOutput receiverOutput;

    CotReceiverThread(CotReceiver receiver, boolean[] choices) {
        this.receiver = receiver;
        this.choices = choices;
    }

    CotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(choices.length, choices.length);
            receiverOutput = receiver.receive(choices);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
