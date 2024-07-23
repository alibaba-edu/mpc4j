package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * GF2K-NC-VODE sender thread.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
class Gf2kNcVodeSenderThread extends Thread {
    /**
     * sender
     */
    private final Gf2kNcVodeSender sender;
    /**
     * field
     */
    private final Dgf2k field;
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
    private Gf2kVodeSenderOutput senderOutput;

    Gf2kNcVodeSenderThread(Gf2kNcVodeSender sender, Dgf2k field, int num, int round) {
        this.sender = sender;
        this.field = field;
        this.num = num;
        this.round = round;
    }

    Gf2kVodeSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(field.getSubfieldL(), num);
            for (int index = 0; index < round; index++) {
                if (senderOutput == null) {
                    senderOutput = sender.send();
                } else {
                    senderOutput.merge(sender.send());
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
