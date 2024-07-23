package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeFactory.Gf2kNcVodeType;

/**
 * GF2K-NC-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
public interface Gf2kNcVodeConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    Gf2kNcVodeType getPtoType();

    /**
     * Gets the max num.
     *
     * @return the max num.
     */
    int maxNum();
}
