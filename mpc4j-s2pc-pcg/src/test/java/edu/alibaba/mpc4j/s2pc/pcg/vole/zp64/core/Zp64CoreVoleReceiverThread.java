package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleReceiverOutput;

/**
 * ZP64_VOLE协议接收方线程。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
class Zp64CoreVoleReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final Zp64CoreVoleReceiver receiver;
    /**
     * 素数域
     */
    private final long prime;
    /**
     * 关联值Δ
     */
    private final long delta;
    /**
     * 数量
     */
    private final int num;
    /**
     * 接收方输出
     */
    private Zp64VoleReceiverOutput receiverOutput;

    Zp64CoreVoleReceiverThread(Zp64CoreVoleReceiver receiver, long prime, long delta, int num) {
        this.receiver = receiver;
        this.prime = prime;
        this.delta = delta;
        this.num = num;
    }

    Zp64VoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(prime, delta, num);
            receiverOutput = receiver.receive(num);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
