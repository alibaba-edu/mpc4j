package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleSenderOutput;

/**
 * ZP64-VOLE协议发送方线程。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
class Zp64CoreVoleSenderThread extends Thread {
    /**
     * 接收方
     */
    private final Zp64CoreVoleSender sender;
    /**
     * 素数p
     */
    private final long prime;
    /**
     * x
     */
    private final long[] x;
    /**
     * 接收方输出
     */
    private Zp64VoleSenderOutput senderOutput;

    Zp64CoreVoleSenderThread(Zp64CoreVoleSender sender, long prime, long[] x) {
        this.sender = sender;
        this.prime = prime;
        this.x = x;
    }

    Zp64VoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(prime, x.length);
            senderOutput = sender.send(x);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
