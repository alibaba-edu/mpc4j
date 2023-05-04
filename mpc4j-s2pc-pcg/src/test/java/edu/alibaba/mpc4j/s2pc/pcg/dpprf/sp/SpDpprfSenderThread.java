package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * single-point DPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
class SpDpprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final SpDpprfSender sender;
    /**
     * α bound
     */
    private final int alphaBound;
    /**
     * pre-computed sender output
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private SpDpprfSenderOutput senderOutput;

    SpDpprfSenderThread(SpDpprfSender sender, int alphaBound) {
        this(sender, alphaBound, null);
    }

    SpDpprfSenderThread(SpDpprfSender sender, int alphaBound, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.alphaBound = alphaBound;
        this.preSenderOutput = preSenderOutput;
    }

    SpDpprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(alphaBound);
            senderOutput = preSenderOutput == null ?
                sender.puncture(alphaBound) : sender.puncture(alphaBound, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
