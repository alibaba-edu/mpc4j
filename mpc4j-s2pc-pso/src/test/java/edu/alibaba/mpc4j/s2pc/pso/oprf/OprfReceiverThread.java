package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * OPRF协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
class OprfReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final OprfReceiver oprfReceiver;
    /**
     * 输入
     */
    private final byte[][] inputs;
    /**
     * 输出
     */
    private OprfReceiverOutput receiverOutput;

    OprfReceiverThread(OprfReceiver oprfReceiver, byte[][] inputs) {
        this.oprfReceiver = oprfReceiver;
        this.inputs = inputs;
    }

    OprfReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            oprfReceiver.getRpc().connect();
            oprfReceiver.init(inputs.length);
            receiverOutput = oprfReceiver.oprf(inputs);
            oprfReceiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}