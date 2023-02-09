package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.AbstractDpprfSender;

/**
 * Correlated DPPRF abstract sender.
 *
 * @author Weiran Liu
 * @date 2022/12/31
 */
public abstract class AbstractCdpprfSender extends AbstractDpprfSender implements CdpprfSender {
    /**
     * Δ
     */
    protected byte[] delta;

    protected AbstractCdpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, CdpprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(byte[] delta, int maxBatchNum, int maxAlphaBound) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "Δ byte length must be " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + delta.length;
        // copy Δ
        this.delta = BytesUtils.clone(delta);
        setInitInput(maxBatchNum, maxAlphaBound);
    }
}
