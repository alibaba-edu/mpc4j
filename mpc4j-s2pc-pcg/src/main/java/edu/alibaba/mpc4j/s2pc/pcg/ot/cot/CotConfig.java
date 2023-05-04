package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * COT config.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public interface CotConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    CotFactory.CotType getPtoType();
}
