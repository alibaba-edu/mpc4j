package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;

/**
 * Batched Share Translation sender output.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
public class BstSenderOutput extends AbstractBatchPcgOutput {
    /**
     * sender outputs
     */
    private final SstSenderOutput[] senderOutputs;
    /**
     * element byte length
     */
    private final int byteLength;

    public BstSenderOutput(SstSenderOutput[] senderOutputs) {
        super(senderOutputs);
        byteLength = senderOutputs[0].getByteLength();
        this.senderOutputs = senderOutputs;
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
    public SstSenderOutput get(int index) {
        return senderOutputs[index];
    }
}
