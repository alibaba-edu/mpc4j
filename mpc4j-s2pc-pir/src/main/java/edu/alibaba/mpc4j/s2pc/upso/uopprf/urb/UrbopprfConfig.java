package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * unbalanced related-Batch OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface UrbopprfConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    UrbopprfFactory.UrbopprfType getPtoType();

    /**
     * Gets the number of PRF outputs for the receiver's input.
     *
     * @return the number of PRF outputs for the receiver's input.
     */
    int getD();
}
