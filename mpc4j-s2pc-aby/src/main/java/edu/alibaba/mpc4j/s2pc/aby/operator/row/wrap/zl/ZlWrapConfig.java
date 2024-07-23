package edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl wrap protocol config.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public interface ZlWrapConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return protocol type.
     */
    ZlWrapFactory.ZlWrapType getPtoType();
}
