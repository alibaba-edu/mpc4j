package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp;

import java.util.stream.IntStream;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotSenderOutput;

/**
 * Batched single-point COT sender output.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class BspCotSenderOutput extends AbstractBatchPcgOutput {
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * sender outputs
     */
    private final SspCotSenderOutput[] senderOutputs;

    public BspCotSenderOutput(SspCotSenderOutput[] senderOutputs) {
        super(senderOutputs);
        // get Δ
        delta = BlockUtils.clone(senderOutputs[0].getDelta());
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            SspCotSenderOutput senderOutput = senderOutputs[batchIndex];
            Preconditions.checkArgument(BlockUtils.equals(delta, senderOutput.getDelta()));
        });
        this.senderOutputs = senderOutputs;
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
    public SspCotSenderOutput get(int index) {
        return senderOutputs[index];
    }
}
