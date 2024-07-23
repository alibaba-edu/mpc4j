package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * standard keyword PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/7/19
 */
public interface StdKwPirConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    StdKwPirFactory.StdKwPirType getPtoType();
}
