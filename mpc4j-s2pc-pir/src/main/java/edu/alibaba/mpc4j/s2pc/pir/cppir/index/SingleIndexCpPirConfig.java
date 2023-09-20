package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory.SingleIndexCpPirType;

/**
 * Single Index Client-specific Preprocessing PIR config.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public interface SingleIndexCpPirConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    SingleIndexCpPirType getProType();
}
