package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Z2 circuit config.
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public interface Z2cConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    Z2cFactory.BcType getPtoType();
}
