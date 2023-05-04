package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Single single-point GF2K VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface Gf2kSspVoleConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    Gf2kSspVoleFactory.Gf2kSspVoleType getPtoType();
}
