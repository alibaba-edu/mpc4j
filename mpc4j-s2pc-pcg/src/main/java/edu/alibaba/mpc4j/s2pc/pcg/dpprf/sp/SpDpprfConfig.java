package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * single-point DPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface SpDpprfConfig extends MultiPartyPtoConfig {
    /**
     * Get the type.
     *
     * @return the type.
     */
    SpDpprfFactory.SpDpprfType getPtoType();
}
