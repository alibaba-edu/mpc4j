package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * sing-query OPRF config.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public interface SqOprfConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    SqOprfFactory.SqOprfType getPtoType();
}
