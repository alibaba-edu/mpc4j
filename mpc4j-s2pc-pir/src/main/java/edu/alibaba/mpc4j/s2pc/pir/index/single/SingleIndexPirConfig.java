package edu.alibaba.mpc4j.s2pc.pir.index.single;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Single Index PIR config interface.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface SingleIndexPirConfig extends MultiPartyPtoConfig {
    /**
     * protocol type.
     *
     * @return protocol type.
     */
    SingleIndexPirFactory.SingleIndexPirType getProType();
}
