package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVolePartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleSenderOutput;

/**
 * Batch single-point GF2K-VOLE sender output.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public class Gf2kBspVoleSenderOutput extends AbstractBatchPcgOutput implements Gf2kVolePartyOutput {
    /**
     * field
     */
    private Sgf2k field;
    /**
     * sender outputs
     */
    private final Gf2kSspVoleSenderOutput[] senderOutputs;

    public Gf2kBspVoleSenderOutput(Gf2kSspVoleSenderOutput[] senderOutputs) {
        super(senderOutputs);
        this.senderOutputs = senderOutputs;
    }

    @Override
    public Gf2kSspVoleSenderOutput get(int index) {
        return senderOutputs[index];
    }

    @Override
    public Sgf2k getField() {
        return field;
    }
}
