package edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl Cross Term Multiplication Config.
 *
 * @author Liqiang Peng
 * @date 2024/6/5
 */
public interface ZlCrossTermConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlCrossTermFactory.ZlCrossTermType getPtoType();
}
