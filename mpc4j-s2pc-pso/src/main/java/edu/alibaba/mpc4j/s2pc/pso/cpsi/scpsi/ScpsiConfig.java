package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * server-payload circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface ScpsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    ScpsiFactory.ScpsiType getPtoType();
}
