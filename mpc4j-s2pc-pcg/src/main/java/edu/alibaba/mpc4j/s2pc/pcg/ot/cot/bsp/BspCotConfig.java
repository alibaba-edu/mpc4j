package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Batched single-point COT config.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public interface BspCotConfig extends MultiPartyPtoConfig {
    /**
     * Gets type.
     *
     * @return type.
     */
    BspCotFactory.BspCotType getPtoType();
}
