package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * aid PSI config.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public interface AidPsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    AidPsiFactory.AidPsiType getPtoType();
}
