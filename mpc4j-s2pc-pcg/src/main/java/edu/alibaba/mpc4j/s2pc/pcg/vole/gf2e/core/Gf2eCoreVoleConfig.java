package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * GF2E-core VOLE config.
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public interface Gf2eCoreVoleConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    Gf2eCoreVoleFactory.Gf2eCoreVoleType getPtoType();
}
