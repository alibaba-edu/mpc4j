package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodePartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeReceiverOutput;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * GF2K-BSP-VODE receiver output.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public class Gf2kBspVodeReceiverOutput extends AbstractBatchPcgOutput implements Gf2kVodePartyOutput {
    /**
     * field
     */
    private final Dgf2k field;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * receiver outputs
     */
    private final Gf2kSspVodeReceiverOutput[] receiverOutputs;

    public Gf2kBspVodeReceiverOutput(Gf2kSspVodeReceiverOutput[] receiverOutputs) {
        super(receiverOutputs);
        // get Δ
        delta = receiverOutputs[0].getDelta();
        // get field
        field = receiverOutputs[0].getField();
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            Gf2kSspVodeReceiverOutput receiverOutput = receiverOutputs[batchIndex];
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
    public Gf2kSspVodeReceiverOutput get(int index) {
        return receiverOutputs[index];
    }

    @Override
    public Dgf2k getField() {
        return field;
    }
}
