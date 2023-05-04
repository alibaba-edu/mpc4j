package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleSenderOutput;

import java.math.BigInteger;

/**
 * ZP-core VOLE sender thread.
 *
 * @author Hanwen Feng
 * @date 2022/06/10
 */
class ZpCoreVoleSenderThread extends Thread {
    /**
     * the sender
     */
    private final ZpCoreVoleSender sender;
    /**
     * the Zp instance
     */
    private final Zp zp;
    /**
     * x
     */
    private final BigInteger[] x;
    /**
     * the sender output
     */
    private ZpVoleSenderOutput senderOutput;

    ZpCoreVoleSenderThread(ZpCoreVoleSender sender, Zp zp, BigInteger[] x) {
        this.sender = sender;
        this.zp = zp;
        this.x = x;
    }

    ZpVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(zp, x.length);
            senderOutput = sender.send(x);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
