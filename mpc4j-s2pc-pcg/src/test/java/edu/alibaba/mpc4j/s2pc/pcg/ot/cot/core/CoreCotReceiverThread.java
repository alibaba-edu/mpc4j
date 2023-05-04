package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * 核COT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2021/01/26
 */
class CoreCotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final CoreCotReceiver receiver;
    /**
     * 选择比特
     */
    private final boolean[] choices;
    /**
     * 输出
     */
    private CotReceiverOutput receiverOutput;

    CoreCotReceiverThread(CoreCotReceiver receiver, boolean[] choices) {
        this.receiver = receiver;
        this.choices = choices;
    }

    CotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(choices.length);
            receiverOutput = receiver.receive(choices);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}