package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Unbalanced Circuit PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public interface UcpsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    UcpsiFactory.UcpsiType getPtoType();
}
