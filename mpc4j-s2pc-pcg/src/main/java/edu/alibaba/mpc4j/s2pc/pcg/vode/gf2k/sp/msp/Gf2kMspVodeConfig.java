package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeFactory.Gf2kMspVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.Gf2kMspVoleFactory.Gf2kMspVoleType;

/**
 * GF2K-MSP-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public interface Gf2kMspVodeConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    Gf2kMspVodeType getPtoType();

    /**
     * Gets GF2K-BSP-VODE config.
     *
     * @return GF2K-BSP-VODE config.
     */
    Gf2kBspVodeConfig getGf2kBspVodeConfig();
}
