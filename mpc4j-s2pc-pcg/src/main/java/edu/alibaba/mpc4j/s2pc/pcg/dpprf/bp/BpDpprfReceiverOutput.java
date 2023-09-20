package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.s2pc.pcg.BatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiverOutput;

import java.util.Arrays;

/**
 * batch-point DPPRF receiver output.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class BpDpprfReceiverOutput implements BatchPcgOutput {
    /**
     * α upper bound
     */
    private final int alphaBound;
    /**
     * α bit length
     */
    private final int h;
    /**
     * batched DPPRF receiver outputs
     */
    private final SpDpprfReceiverOutput[] receiverOutputs;

    public BpDpprfReceiverOutput(SpDpprfReceiverOutput[] receiverOutputs) {
        int num = receiverOutputs.length;
        assert num > 0 : "num must be greater than 0: " + num;
        // set global parameters using the first sender output
        SpDpprfReceiverOutput firstReceiverOutput = receiverOutputs[0];
        this.alphaBound = firstReceiverOutput.getAlphaBound();
        h = firstReceiverOutput.getH();
        this.receiverOutputs = Arrays.stream(receiverOutputs)
            .peek(receiverOutput -> {
                assert receiverOutput.getAlphaBound() == alphaBound
                    : "each α bound must be " + alphaBound + ": " + receiverOutput.getAlphaBound();
                assert receiverOutput.getH() == h : "each h must be " + h + ": " + receiverOutput.getH();
            })
            .toArray(SpDpprfReceiverOutput[]::new);
    }

    /**
     * Get α bit length.
     *
     * @return α bit length.
     */
    public int getH() {
        return h;
    }

    /**
     * Get α upper bound.
     *
     * @return α upper bound.
     */
    public int getAlphaBound() {
        return alphaBound;
    }

    @Override
    public int getBatchNum() {
        return receiverOutputs.length;
    }

    /**
     * Get single-point DPPRF receiver output.
     *
     * @param index the index.
     * @return the single-point DPPRF receiver output.
     */
    public SpDpprfReceiverOutput getSpDpprfReceiverOutput(int index) {
        return receiverOutputs[index];
    }
}
