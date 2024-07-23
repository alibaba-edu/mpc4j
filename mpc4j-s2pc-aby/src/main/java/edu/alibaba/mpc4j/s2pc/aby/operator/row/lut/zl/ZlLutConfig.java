package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl lookup table protocol config.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public interface ZlLutConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return protocol type.
     */
    ZlLutFactory.ZlLutType getPtoType();
}
