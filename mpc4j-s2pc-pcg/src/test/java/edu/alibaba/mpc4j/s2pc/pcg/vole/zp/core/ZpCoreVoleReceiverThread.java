package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleReceiverOutput;

import java.math.BigInteger;

/**
 * ZP-core VOLE receiver thread.
 *
 * @author Hanwen Feng
 * @date 2022/06/10
 */
class ZpCoreVoleReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final ZpCoreVoleReceiver receiver;
    /**
     * the Zp instance
     */
    private final Zp zp;
    /**
     * Δ
     */
    private final BigInteger delta;
    /**
     * num
     */
    private final int num;
    /**
     * the receiver output
     */
    private ZpVoleReceiverOutput receiverOutput;

    ZpCoreVoleReceiverThread(ZpCoreVoleReceiver receiver, Zp zp, BigInteger delta, int num) {
        this.receiver = receiver;
        this.zp = zp;
        this.delta = delta;
        this.num = num;
    }

    ZpVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(zp, delta, num);
            receiverOutput = receiver.receive(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
