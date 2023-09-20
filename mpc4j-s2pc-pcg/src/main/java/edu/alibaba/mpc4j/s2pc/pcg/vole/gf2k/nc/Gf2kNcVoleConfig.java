package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * no-choice GF2K-VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public interface Gf2kNcVoleConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    Gf2kNcVoleFactory.Gf2kNcVoleType getPtoType();

    /**
     * Gets the max num.
     *
     * @return the max num.
     */
    int maxNum();
}
