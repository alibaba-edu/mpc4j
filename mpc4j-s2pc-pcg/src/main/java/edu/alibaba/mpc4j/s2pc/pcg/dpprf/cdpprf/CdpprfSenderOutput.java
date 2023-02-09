package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfSenderOutput;

/**
 * Correlated Distributed Puncturable PRF (CDPPRF) sender output.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public class CdpprfSenderOutput extends DpprfSenderOutput {
    /**
     * correlated Δ
     */
    private final byte[] delta;

    public CdpprfSenderOutput(byte[] delta, int alphaBound, byte[][][] prfArrays) {
        super(alphaBound, prfArrays);
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "Δ byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + delta.length;
        this.delta = BytesUtils.clone(delta);
    }

    /**
     * Get the correlated Δ.
     *
     * @return the correlated Δ.
     */
    public byte[] getDelta() {
        return delta;
    }
}
