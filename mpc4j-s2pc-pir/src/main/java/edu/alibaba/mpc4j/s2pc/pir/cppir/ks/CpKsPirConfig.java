package edu.alibaba.mpc4j.s2pc.pir.cppir.ks;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory.CpKsPirType;

/**
 * client-specific preprocessing KSPIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public interface CpKsPirConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    CpKsPirType getPtoType();
}
