package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSenderOutput;

/**
 * batch-point RDPPRF sender output.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public class BpRdpprfSenderOutput extends AbstractBatchPcgOutput {
    /**
     * sender outputs
     */
    private final SpRdpprfSenderOutput[] senderOutputs;

    public BpRdpprfSenderOutput(SpRdpprfSenderOutput[] senderOutputs) {
        super(senderOutputs);
        this.senderOutputs = senderOutputs;
    }

    @Override
    public SpRdpprfSenderOutput get(int index) {
        return senderOutputs[index];
    }
}
