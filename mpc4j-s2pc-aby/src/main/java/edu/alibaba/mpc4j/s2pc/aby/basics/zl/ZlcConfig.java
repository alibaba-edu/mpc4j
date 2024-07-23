package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory.ZlcType;

/**
 * Zl config.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public interface ZlcConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    ZlcType getPtoType();

    /**
     * Gets default round num.
     *
     * @param l l.
     * @return default round num.
     */
    int defaultRoundNum(int l);
}
