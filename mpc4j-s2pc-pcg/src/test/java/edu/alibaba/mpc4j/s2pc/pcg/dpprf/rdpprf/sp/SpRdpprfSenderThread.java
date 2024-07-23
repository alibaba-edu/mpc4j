package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * single-point RDPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
class SpRdpprfSenderThread extends Thread {
    /**
     * sender
     */
    private final SpRdpprfSender sender;
    /**
     * n
     */
    private final int num;
    /**
     * pre-computed sender output
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private SpRdpprfSenderOutput senderOutput;

    SpRdpprfSenderThread(SpRdpprfSender sender, int num) {
        this(sender, num, null);
    }

    SpRdpprfSenderThread(SpRdpprfSender sender, int num, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    SpRdpprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.puncture(num, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
