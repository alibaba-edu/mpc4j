package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiverOutput;

/**
 * batch-point RDPPRF receiver output.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class BpRdpprfReceiverOutput extends AbstractBatchPcgOutput {
    /**
     * receiver outputs
     */
    private final SpRdpprfReceiverOutput[] receiverOutputs;

    public BpRdpprfReceiverOutput(SpRdpprfReceiverOutput[] receiverOutputs) {
        super(receiverOutputs);
        this.receiverOutputs = receiverOutputs;
    }

    @Override
    public SpRdpprfReceiverOutput get(int index) {
        return receiverOutputs[index];
    }
}
