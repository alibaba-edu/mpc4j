package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * batch-point DPPRF config.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public interface BpDpprfConfig extends MultiPartyPtoConfig {
    /**
     * Get the type.
     *
     * @return the type.
     */
    BpDpprfFactory.BpDpprfType getPtoType();
}
