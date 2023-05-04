package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * GF2K-core VOLE config.。
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface Gf2kCoreVoleConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    Gf2kCoreVoleFactory.Gf2kCoreVoleType getPtoType();
}
