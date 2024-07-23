package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory.Gf2kCoreVodeType;

/**
 * GF2K-core-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public interface Gf2kCoreVodeConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    Gf2kCoreVodeType getPtoType();
}
