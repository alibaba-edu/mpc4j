package edu.alibaba.mpc4j.s2pc.pso.payablepsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Payable PSI config interface.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
public interface PayablePsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    PayablePsiFactory.PayablePsiType getPtoType();
}
