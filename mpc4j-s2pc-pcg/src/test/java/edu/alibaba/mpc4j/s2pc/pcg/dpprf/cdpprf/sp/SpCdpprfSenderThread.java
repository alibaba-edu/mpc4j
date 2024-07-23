package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * SP-CDPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
class SpCdpprfSenderThread extends Thread {
    /**
     * sender
     */
    private final SpCdpprfSender sender;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * num
     */
    private final int num;
    /**
     * pre-computed COT sender output
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private SpCdpprfSenderOutput senderOutput;

    SpCdpprfSenderThread(SpCdpprfSender sender, byte[] delta, int num) {
        this(sender, delta, num, null);
    }

    SpCdpprfSenderThread(SpCdpprfSender sender, byte[] delta, int num, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.delta = delta;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    SpCdpprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(delta);
            senderOutput = sender.puncture(num, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
