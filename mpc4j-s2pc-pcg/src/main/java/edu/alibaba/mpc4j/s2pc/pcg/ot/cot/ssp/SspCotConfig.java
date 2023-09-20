package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Single single-point COT config.
 *
 * @author Weiran Liu
 * @date 2023/7/13
 */
public interface SspCotConfig extends MultiPartyPtoConfig {
    /**
     * Gets type.
     *
     * @return type.
     */
    SspCotFactory.SspCotType getPtoType();
}
