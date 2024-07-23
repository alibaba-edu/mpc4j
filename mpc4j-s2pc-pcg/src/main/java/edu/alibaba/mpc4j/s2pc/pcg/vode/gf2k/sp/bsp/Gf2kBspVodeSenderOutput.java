package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodePartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeSenderOutput;

/**
 * GF2K-BSP-VODE sender output.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Gf2kBspVodeSenderOutput extends AbstractBatchPcgOutput implements Gf2kVodePartyOutput {
    /**
     * field
     */
    private Dgf2k field;
    /**
     * sender outputs
     */
    private final Gf2kSspVodeSenderOutput[] senderOutputs;

    public Gf2kBspVodeSenderOutput(Gf2kSspVodeSenderOutput[] senderOutputs) {
        super(senderOutputs);
        this.senderOutputs = senderOutputs;
    }

    @Override
    public Gf2kSspVodeSenderOutput get(int index) {
        return senderOutputs[index];
    }

    @Override
    public Dgf2k getField() {
        return field;
    }
}
