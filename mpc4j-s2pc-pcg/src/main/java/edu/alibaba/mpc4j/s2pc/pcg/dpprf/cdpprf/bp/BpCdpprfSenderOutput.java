package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.AbstractBatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfSenderOutput;

import java.util.stream.IntStream;

/**
 * BP-CDPPRF sender output.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class BpCdpprfSenderOutput extends AbstractBatchPcgOutput {
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * sender outputs
     */
    private final SpCdpprfSenderOutput[] senderOutputs;

    public BpCdpprfSenderOutput(SpCdpprfSenderOutput[] senderOutputs) {
        super(senderOutputs);
        // get Δ
        delta = BlockUtils.clone(senderOutputs[0].getDelta());
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            SpCdpprfSenderOutput senderOutput = senderOutputs[batchIndex];
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
    public SpCdpprfSenderOutput get(int index) {
        return senderOutputs[index];
    }
}
