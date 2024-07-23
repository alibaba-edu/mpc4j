package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * BP-CDPPRF sender thread.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
class BpCdpprfSenderThread extends Thread {
    /**
     * sender
     */
    private final BpCdpprfSender sender;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * batch num
     */
    private final int batchNum;
    /**
     * each num
     */
    private final int eachNum;
    /**
     * pre-computed COT sender output
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * sender output
     */
    private BpCdpprfSenderOutput senderOutput;

    BpCdpprfSenderThread(BpCdpprfSender sender, byte[] delta, int batchNum, int eachNum) {
        this(sender, delta, batchNum, eachNum, null);
    }

    BpCdpprfSenderThread(BpCdpprfSender sender, byte[] delta, int batchNum, int eachNum, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.delta = delta;
        this.batchNum = batchNum;
        this.eachNum = eachNum;
        this.preSenderOutput = preSenderOutput;
    }

    BpCdpprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(delta);
            senderOutput = sender.puncture(batchNum, eachNum, preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
