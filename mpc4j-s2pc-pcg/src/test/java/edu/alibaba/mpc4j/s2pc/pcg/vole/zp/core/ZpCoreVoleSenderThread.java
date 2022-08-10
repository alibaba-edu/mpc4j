package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleSenderOutput;

import java.math.BigInteger;

/**
 * ZP-核VOLE协议发送方线程。
 *
 * @author Hanwen Feng
 * @date 2022/06/10
 */
class ZpCoreVoleSenderThread extends Thread {
    /**
     * 发送方
     */
    private final ZpCoreVoleSender sender;
    /**
     * 素数p
     */
    private final BigInteger prime;
    /**
     * x
     */
    private final BigInteger[] x;
    /**
     * 接收方输出
     */
    private ZpVoleSenderOutput senderOutput;

    ZpCoreVoleSenderThread(ZpCoreVoleSender sender, BigInteger prime, BigInteger[] x) {
        this.sender = sender;
        this.prime = prime;
        this.x = x;
    }

    ZpVoleSenderOutput getSenderOutput() {
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
