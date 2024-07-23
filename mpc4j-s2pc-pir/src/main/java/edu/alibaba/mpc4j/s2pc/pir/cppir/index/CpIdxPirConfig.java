package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * client-specific preprocessing index PIR config.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public interface CpIdxPirConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    CpIdxPirType getPtoType();
}
