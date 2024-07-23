package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
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
     * field
     */
    private final Sgf2k field;
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
    private Gf2kVoleSenderOutput senderOutput;

    Gf2kNcVoleSenderThread(Gf2kNcVoleSender sender, Sgf2k field, int num, int round) {
        this.sender = sender;
        this.field = field;
        this.num = num;
        this.round = round;
    }

    Gf2kVoleSenderOutput getSenderOutput() {
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
