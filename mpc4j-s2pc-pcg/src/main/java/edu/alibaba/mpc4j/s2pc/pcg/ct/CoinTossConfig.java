package edu.alibaba.mpc4j.s2pc.pcg.ct;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * coin-tossing protocol config.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public interface CoinTossConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    CoinTossFactory.CoinTossType getPtoType();
}
