package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory.StdIdxPirType;

/**
 * standard index PIR config.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public interface StdIdxPirConfig extends MultiPartyPtoConfig {
    /**
     * protocol type.
     *
     * @return protocol type.
     */
    StdIdxPirType getProType();
}
