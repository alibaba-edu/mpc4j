package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;

/**
 * Z2-SSP-VOLE发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/6/17
 */
class Z2SspVoleSenderThread extends Thread {
    /**
     * 发送方
     */
    private final Z2SspVoleSender sender;
    /**
     * α
     */
    private final int alpha;
    /**
     * 数量
     */
    private final int num;
    /**
     * 预计算发送方输出
     */
    private final Z2VoleSenderOutput preSenderOutput;
    /**
     * 输出
     */
    private Z2SspVoleSenderOutput senderOutput;

    Z2SspVoleSenderThread(Z2SspVoleSender sender, int alpha, int num) {
        this(sender, alpha, num, null);
    }

    Z2SspVoleSenderThread(Z2SspVoleSender sender, int alpha, int num, Z2VoleSenderOutput preSenderOutput) {
        this.sender = sender;
        this.alpha = alpha;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    Z2SspVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(num);
            senderOutput = preSenderOutput == null ? sender.send(alpha, num) : sender.send(alpha, num, preSenderOutput);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
