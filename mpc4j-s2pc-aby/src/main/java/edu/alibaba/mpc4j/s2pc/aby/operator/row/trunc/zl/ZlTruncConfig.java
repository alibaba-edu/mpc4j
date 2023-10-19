package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl Truncation Config.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public interface ZlTruncConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlTruncFactory.ZlTruncType getPtoType();
}
