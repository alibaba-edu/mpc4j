package edu.alibaba.mpc4j.s2pc.opf.opprf.batch;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Batch OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public interface BopprfConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    BopprfFactory.BopprfType getPtoType();
}
