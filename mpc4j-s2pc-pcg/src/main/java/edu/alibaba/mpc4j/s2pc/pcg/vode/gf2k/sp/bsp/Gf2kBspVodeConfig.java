package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory.Gf2kBspVodeType;

/**
 * GF2K-BSP-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public interface Gf2kBspVodeConfig extends MultiPartyPtoConfig {
    /**
     * Gets type.
     *
     * @return type.
     */
    Gf2kBspVodeType getPtoType();
}
