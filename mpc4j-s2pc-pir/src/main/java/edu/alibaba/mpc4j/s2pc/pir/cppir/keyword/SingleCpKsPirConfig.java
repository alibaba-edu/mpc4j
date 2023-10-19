package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleCpKsPirFactory.SingleCpKsPirType;

/**
 * Single client-specific preprocessing KSPIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public interface SingleCpKsPirConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    SingleCpKsPirType getPtoType();
}
