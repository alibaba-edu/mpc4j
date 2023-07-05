package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl mux config.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface ZlMuxConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlMuxFactory.ZlMuxType getPtoType();
}
