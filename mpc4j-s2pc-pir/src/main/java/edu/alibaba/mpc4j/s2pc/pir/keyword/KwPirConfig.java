package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * PIR config interface.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirConfig extends MultiPartyPtoConfig {
    /**
     * protocol type.
     *
     * @return protocol type.
     */
    KwPirFactory.KwPirType getProType();
}
