package edu.alibaba.mpc4j.s2pc.pso.psica;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * PSI Cardinality config.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public interface PsiCaConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PsiCaFactory.PsiCaType getPtoType();
}