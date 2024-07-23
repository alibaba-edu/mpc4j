package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleFactory.Gf2kSspVoleType;

/**
 * Single single-point GF2K-VOLE config.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface Gf2kSspVoleConfig extends MultiPartyPtoConfig {
    /**
     * Gets type.
     *
     * @return type.
     */
    Gf2kSspVoleType getPtoType();
}