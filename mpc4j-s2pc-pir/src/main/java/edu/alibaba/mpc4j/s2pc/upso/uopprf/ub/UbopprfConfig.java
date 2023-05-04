package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * unbalanced batched OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public interface UbopprfConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    UbopprfFactory.UbopprfType getPtoType();
}
