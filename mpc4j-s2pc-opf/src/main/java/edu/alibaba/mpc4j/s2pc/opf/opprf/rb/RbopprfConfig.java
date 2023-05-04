package edu.alibaba.mpc4j.s2pc.opf.opprf.rb;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Related-Batch OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface RbopprfConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    RbopprfFactory.RbopprfType getPtoType();

    /**
     * Gets the number of PRF outputs for the receiver's input.
     *
     * @return the number of PRF outputs for the receiver's input.
     */
    int getD();
}
