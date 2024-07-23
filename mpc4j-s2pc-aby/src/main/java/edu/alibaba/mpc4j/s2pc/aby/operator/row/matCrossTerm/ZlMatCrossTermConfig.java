package edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl Matrix Cross Term Multiplication Config.
 *
 * @author Liqiang Peng
 * @date 2024/6/7
 */
public interface ZlMatCrossTermConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlMatCrossTermFactory.ZlMatCrossTermType getPtoType();
}
