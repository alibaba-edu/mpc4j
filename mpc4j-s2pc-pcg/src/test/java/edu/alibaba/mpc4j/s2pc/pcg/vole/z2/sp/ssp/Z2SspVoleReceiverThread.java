package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;

/**
 * Z2-SSP-VOLE接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/6/17
 */
class Z2SspVoleReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final Z2SspVoleReceiver receiver;
    /**
     * 关联值Δ
     */
    private final boolean delta;
    /**
     * 数量
     */
    private final int num;
    /**
     * 预计算接收方输出
     */
    private final Z2VoleReceiverOutput preReceiverOutput;
    /**
     * 输出
     */
    private Z2SspVoleReceiverOutput receiverOutput;

    Z2SspVoleReceiverThread(Z2SspVoleReceiver receiver, boolean delta, int num) {
        this(receiver, delta, num, null);
    }

    Z2SspVoleReceiverThread(Z2SspVoleReceiver receiver, boolean delta, int num, Z2VoleReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.delta = delta;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    Z2SspVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(delta, num);
            receiverOutput = preReceiverOutput == null ? receiver.receive(num) : receiver.receive(num, preReceiverOutput);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
