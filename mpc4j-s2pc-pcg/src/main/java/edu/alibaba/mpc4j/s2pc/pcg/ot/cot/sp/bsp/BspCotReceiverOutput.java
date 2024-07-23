package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp;

import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotReceiverOutput;

/**
 * Batched single-point COT receiver output.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class BspCotReceiverOutput extends AbstractBatchPcgOutput {
    /**
     * receiver outputs
     */
    private final SspCotReceiverOutput[] receiverOutputs;

    public BspCotReceiverOutput(SspCotReceiverOutput[] receiverOutputs) {
        super(receiverOutputs);
        this.receiverOutputs = receiverOutputs;
    }

    @Override
    public SspCotReceiverOutput get(int index) {
        return receiverOutputs[index];
    }
}
