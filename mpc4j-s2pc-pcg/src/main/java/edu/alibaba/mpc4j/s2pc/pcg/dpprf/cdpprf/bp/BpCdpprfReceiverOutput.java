package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfReceiverOutput;

/**
 * BP-CDPPRF receiver output.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class BpCdpprfReceiverOutput extends AbstractBatchPcgOutput {
    /**
     * receiver outputs
     */
    private final SpCdpprfReceiverOutput[] receiverOutputs;

    public BpCdpprfReceiverOutput(SpCdpprfReceiverOutput[] receiverOutputs) {
        super(receiverOutputs);
        this.receiverOutputs = receiverOutputs;
    }

    @Override
    public SpCdpprfReceiverOutput get(int index) {
        return receiverOutputs[index];
    }
}
