package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * batch-point DPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class BpDpprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final BpDpprfSender sender;
    /**
     * batch num
     */
    private final int batchNum;
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
    private BpDpprfSenderOutput senderOutput;

    BpDpprfSenderThread(BpDpprfSender sender, int batchNum, int alphaBound) {
        this(sender, batchNum, alphaBound, null);
    }

    BpDpprfSenderThread(BpDpprfSender sender, int batchNum, int alphaBound, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.batchNum = batchNum;
        this.alphaBound = alphaBound;
        this.preSenderOutput = preSenderOutput;
    }

    BpDpprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(batchNum, alphaBound);
            senderOutput = preSenderOutput == null ? sender.puncture(batchNum, alphaBound)
                : sender.puncture(batchNum, alphaBound, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
