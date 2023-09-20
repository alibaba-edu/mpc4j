package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * GF2K-NC-VOLE sender thread.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
class Gf2kNcVoleSenderThread extends Thread {
    /**
     * sender
     */
    private final Gf2kNcVoleSender sender;
    /**
     * num
     */
    private final int num;
    /**
     * round
     */
    private final int round;
    /**
     * sender output
     */
    private final Gf2kVoleSenderOutput senderOutput;

    Gf2kNcVoleSenderThread(Gf2kNcVoleSender sender, int num, int round) {
        this.sender = sender;
        this.num = num;
        this.round = round;
        senderOutput = Gf2kVoleSenderOutput.createEmpty();
    }

    Gf2kVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            for (int index = 0; index < round; index++) {
                senderOutput.merge(sender.send());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
