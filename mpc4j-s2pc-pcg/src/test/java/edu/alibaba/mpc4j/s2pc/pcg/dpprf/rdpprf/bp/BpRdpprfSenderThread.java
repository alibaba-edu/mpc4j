package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * batch-point RDPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class BpRdpprfSenderThread extends Thread {
    /**
     * the sender
     */
    private final BpRdpprfSender sender;
    /**
     * batch num
     */
    private final int batchNum;
    /**
     * each num
     */
    private final int eachNum;
    /**
     * pre-computed sender output
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private BpRdpprfSenderOutput senderOutput;

    BpRdpprfSenderThread(BpRdpprfSender sender, int batchNum, int eachNum) {
        this(sender, batchNum, eachNum, null);
    }

    BpRdpprfSenderThread(BpRdpprfSender sender, int batchNum, int eachNum, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.batchNum = batchNum;
        this.eachNum = eachNum;
        this.preSenderOutput = preSenderOutput;
    }

    BpRdpprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.puncture(batchNum, eachNum, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
