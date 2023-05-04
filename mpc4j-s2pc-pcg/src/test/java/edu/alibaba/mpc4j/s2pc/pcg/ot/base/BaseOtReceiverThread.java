package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 基础OT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
class BaseOtReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final BaseOtReceiver receiver;
    /**
     * 选择比特
     */
    private final boolean[] choices;
    /**
     * 输出
     */
    private BaseOtReceiverOutput receiverOutput;

    BaseOtReceiverThread(BaseOtReceiver receiver, boolean[] choices) {
        this.receiver = receiver;
        this.choices = choices;
    }

    BaseOtReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init();
            receiverOutput = receiver.receive(choices);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}