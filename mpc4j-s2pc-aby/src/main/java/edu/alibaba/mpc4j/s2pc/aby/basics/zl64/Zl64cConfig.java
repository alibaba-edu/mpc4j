package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.Zl64cFactory.Zl64cType;

/**
 * Zl config.
 *
 * @author Li Peng
 * @date 2024/7/23
 */
public interface Zl64cConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    Zl64cType getPtoType();

    /**
     * Gets default round num.
     *
     * @param l l.
     * @return default round num.
     */
    int defaultRoundNum(int l);
}
