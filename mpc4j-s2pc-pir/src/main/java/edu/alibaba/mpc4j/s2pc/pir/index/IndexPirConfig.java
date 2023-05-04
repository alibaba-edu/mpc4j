package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Index PIR config interface.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public interface IndexPirConfig extends MultiPartyPtoConfig {
    /**
     * protocol type.
     *
     * @return protocol type.
     */
    IndexPirFactory.IndexPirType getProType();
}
