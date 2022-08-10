package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * 预计算COT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
class PreCotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final PreCotReceiver receiver;
    /**
     * 预计算输出
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * 选择比特
     */
    private final boolean[] choices;
    /**
     * 输出
     */
    private CotReceiverOutput receiverOutput;

    PreCotReceiverThread(PreCotReceiver receiver, CotReceiverOutput preReceiverOutput, boolean[] choices) {
        this.receiver = receiver;
        this.preReceiverOutput = preReceiverOutput;
        this.choices = choices;
    }

    CotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init();
            receiverOutput = receiver.receive(preReceiverOutput, choices);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }

    }
}
