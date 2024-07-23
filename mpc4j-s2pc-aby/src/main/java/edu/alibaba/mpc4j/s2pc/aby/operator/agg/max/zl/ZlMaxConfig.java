package edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl Max Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public interface ZlMaxConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlMaxFactory.ZlMaxType getPtoType();
}
