package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 基础n选1-OT协议接收方线程。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
class BaseNotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final BaseNotReceiver receiver;
    /**
     * 最大选择数量
     */
    private final int maxChoice;
    /**
     * 选择数组
     */
    private final int[] choices;
    /**
     * 输出
     */
    private BaseNotReceiverOutput receiverOutput;

    BaseNotReceiverThread(BaseNotReceiver receiver, int[] choices, int maxChoice) {
        this.receiver = receiver;
        this.choices = choices;
        this.maxChoice = maxChoice;
    }

    BaseNotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(maxChoice);
            receiverOutput = receiver.receive(choices);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
