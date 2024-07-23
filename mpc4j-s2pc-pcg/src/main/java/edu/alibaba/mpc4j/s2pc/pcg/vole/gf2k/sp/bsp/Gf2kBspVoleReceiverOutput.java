package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVolePartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleReceiverOutput;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Batch single-point GF2K-VOLE receiver output.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public class Gf2kBspVoleReceiverOutput extends AbstractBatchPcgOutput implements Gf2kVolePartyOutput {
    /**
     * field
     */
    private final Sgf2k field;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * receiver outputs
     */
    private final Gf2kSspVoleReceiverOutput[] receiverOutputs;

    public Gf2kBspVoleReceiverOutput(Gf2kSspVoleReceiverOutput[] receiverOutputs) {
        super(receiverOutputs);
        // get Δ
        delta = receiverOutputs[0].getDelta();
        // get field
        field = receiverOutputs[0].getField();
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            Gf2kSspVoleReceiverOutput receiverOutput = receiverOutputs[batchIndex];
            Preconditions.checkArgument(field.equals(receiverOutput.getField()));
            Preconditions.checkArgument(Arrays.equals(delta, receiverOutput.getDelta()));
        });
        this.receiverOutputs = receiverOutputs;
    }

    /**
     * Gets Δ.
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
    }

    @Override
    public Gf2kSspVoleReceiverOutput get(int index) {
        return receiverOutputs[index];
    }

    @Override
    public Sgf2k getField() {
        return field;
    }
}
