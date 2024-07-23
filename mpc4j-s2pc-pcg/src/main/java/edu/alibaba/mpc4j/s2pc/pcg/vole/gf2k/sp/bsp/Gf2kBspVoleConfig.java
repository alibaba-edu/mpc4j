package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleFactory.Gf2kBspVoleType;

/**
 * Batch single-point GF2K-VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public interface Gf2kBspVoleConfig extends MultiPartyPtoConfig {
    /**
     * Gets type.
     *
     * @return type.
     */
    Gf2kBspVoleType getPtoType();
}
