package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl Corr Config.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public interface ZlCorrConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlCorrFactory.ZlCorrType getPtoType();
}
