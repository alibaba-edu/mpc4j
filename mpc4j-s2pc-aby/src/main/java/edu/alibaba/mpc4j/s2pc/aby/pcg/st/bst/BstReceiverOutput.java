package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;

/**
 * Batched Share Translation receiver output.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
public class BstReceiverOutput extends AbstractBatchPcgOutput {
    /**
     * receiver outputs
     */
    private final SstReceiverOutput[] receiverOutputs;
    /**
     * element byte length
     */
    private final int byteLength;

    public BstReceiverOutput(SstReceiverOutput[] receiverOutputs) {
        super(receiverOutputs);
        byteLength = receiverOutputs[0].getByteLength();
        this.receiverOutputs = receiverOutputs;
    }

    /**
     * Gets element byte length.
     *
     * @return element byte length.
     */
    public int getByteLength() {
        return byteLength;
    }

    @Override
    public SstReceiverOutput get(int index) {
        return receiverOutputs[index];
    }
}
