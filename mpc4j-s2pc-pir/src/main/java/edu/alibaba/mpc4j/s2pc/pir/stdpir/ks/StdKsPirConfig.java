package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * standard KSPIR config.
 *
 * @author Liqiang Peng
 * @date 2024/7/19
 */
public interface StdKsPirConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    StdKsPirFactory.StdKsPirType getPtoType();
}
