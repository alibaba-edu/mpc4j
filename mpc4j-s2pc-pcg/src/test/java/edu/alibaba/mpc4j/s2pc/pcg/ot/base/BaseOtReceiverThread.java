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
    private final BaseOtReceiver baseOtReceiver;
    /**
     * 选择比特
     */
    private final boolean[] choices;
    /**
     * 输出
     */
    private BaseOtReceiverOutput receiverOutput;

    BaseOtReceiverThread(BaseOtReceiver baseOtReceiver, boolean[] choices) {
        this.baseOtReceiver = baseOtReceiver;
        this.choices = choices;
    }

    BaseOtReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            baseOtReceiver.getRpc().connect();
            baseOtReceiver.init();
            receiverOutput = baseOtReceiver.receive(choices);
            baseOtReceiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}