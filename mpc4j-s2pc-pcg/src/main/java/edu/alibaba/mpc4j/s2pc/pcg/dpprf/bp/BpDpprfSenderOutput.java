package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.s2pc.pcg.BatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfSenderOutput;

import java.util.Arrays;

/**
 * batch-point DPPRF sender output.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public class BpDpprfSenderOutput implements BatchPcgOutput {
    /**
     * α upper bound
     */
    private final int alphaBound;
    /**
     * α bit length
     */
    private final int h;
    /**
     * batched DPPRF sender outputs
     */
    private final SpDpprfSenderOutput[] senderOutputs;

    public BpDpprfSenderOutput(SpDpprfSenderOutput[] senderOutputs) {
        int num = senderOutputs.length;
        assert num > 0 : "num must be greater than 0: " + num;
        // set global parameters using the first sender output
        SpDpprfSenderOutput firstSenderOutput = senderOutputs[0];
        this.alphaBound = firstSenderOutput.getAlphaBound();
        h = firstSenderOutput.getH();
        this.senderOutputs = Arrays.stream(senderOutputs)
            .peek(senderOutput -> {
                assert senderOutput.getAlphaBound() == alphaBound
                    : "each α bound must be " + alphaBound + ": " + senderOutput.getAlphaBound();
                assert senderOutput.getH() == h : "each h must be " + h + ": " + senderOutput.getH();
            })
            .toArray(SpDpprfSenderOutput[]::new);
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
        return senderOutputs.length;
    }

    /**
     * Get single-point DPPRF sender output.
     *
     * @param index the index.
     * @return the single-point DPPRF sender output.
     */
    public SpDpprfSenderOutput getSpDpprfSenderOutput(int index) {
        return senderOutputs[index];
    }
}
