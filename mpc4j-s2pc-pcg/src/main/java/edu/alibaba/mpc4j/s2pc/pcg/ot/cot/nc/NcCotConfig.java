package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory.NcCotType;

/**
 * no-choice COT config.
 *
 * @author Weiran Liu
 * @date 2022/01/26
 */
public interface NcCotConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    NcCotType getPtoType();

    /**
     * Gets the max num.
     *
     * @return the max num.
     */
    int maxNum();
}
