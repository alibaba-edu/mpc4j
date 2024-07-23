package edu.alibaba.mpc4j.work.payable.psi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Payable PSI config interface.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public interface PayablePsiConfig extends MultiPartyPtoConfig {

    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    PayablePsiFactory.PayablePsiType getPtoType();
}
