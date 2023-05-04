package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * pre-compute COT sender thread.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
class PreCotSenderThread extends Thread {
    /**
     * the sender
     */
    private final PreCotSender sender;
    /**
     * pre-compute sender output
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * the output
     */
    private CotSenderOutput senderOutput;

    PreCotSenderThread(PreCotSender sender, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.preSenderOutput = preSenderOutput;
    }

    CotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.send(preSenderOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
