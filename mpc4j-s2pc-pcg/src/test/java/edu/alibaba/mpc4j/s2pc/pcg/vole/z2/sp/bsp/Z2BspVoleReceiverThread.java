package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;

/**
 * Z2-SSP-VOLE接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/6/23
 */
class Z2BspVoleReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final Z2BspVoleReceiver receiver;
    /**
     * 关联值Δ
     */
    private final boolean delta;
    /**
     * 批处理数量
     */
    private final int batch;
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
    private Z2BspVoleReceiverOutput receiverOutput;

    Z2BspVoleReceiverThread(Z2BspVoleReceiver receiver, boolean delta, int batch, int num) {
        this(receiver, delta, batch, num, null);
    }

    Z2BspVoleReceiverThread(Z2BspVoleReceiver receiver, boolean delta, int batch, int num,
                            Z2VoleReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.delta = delta;
        this.batch = batch;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    Z2BspVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(delta, batch, num);
            receiverOutput = preReceiverOutput == null ?
                receiver.receive(batch, num) : receiver.receive(batch, num, preReceiverOutput);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
