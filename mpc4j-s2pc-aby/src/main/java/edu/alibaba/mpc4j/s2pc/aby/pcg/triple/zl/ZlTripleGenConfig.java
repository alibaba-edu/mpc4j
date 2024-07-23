package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenFactory.ZlTripleGenType;

/**
 * Zl triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public interface ZlTripleGenConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    ZlTripleGenType getPtoType();

    /**
     * Gets default round num.
     *
     * @param l l.
     * @return default round num.
     */
    int defaultRoundNum(int l);
}
