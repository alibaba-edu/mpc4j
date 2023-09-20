package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory.MspCotType;

/**
 * multi single-point COT config.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public interface MspCotConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    MspCotType getPtoType();

    /**
     * Gets the batched single-point COT config.
     *
     * @return the batched single-point COT config.
     */
    BspCotConfig getBspCotConfig();
}
