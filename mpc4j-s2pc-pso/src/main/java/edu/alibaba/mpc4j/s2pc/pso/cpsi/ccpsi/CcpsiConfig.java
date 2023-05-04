package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * client-payload circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface CcpsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    CcpsiFactory.CcpsiType getPtoType();
}
