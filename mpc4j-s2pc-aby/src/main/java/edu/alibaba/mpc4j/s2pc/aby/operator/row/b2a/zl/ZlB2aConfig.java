package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl boolean to arithmetic protocol config.
 *
 * @author Liqiang Peng
 * @date 2024/6/4
 */
public interface ZlB2aConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return protocol type.
     */
    ZlB2aFactory.ZlB2aType getPtoType();
}
