package edu.alibaba.work.femur;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * PGM-index range keyword PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public interface FemurRpcPirConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    FemurRpcPirFactory.FemurPirType getPtoType();
}
