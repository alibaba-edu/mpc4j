package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenFactory.Zl64TripleGenType;

/**
 * Zl64 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/6/29
 */
public interface Zl64TripleGenConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    Zl64TripleGenType getPtoType();

    /**
     * Gets default round num.
     *
     * @param l l.
     * @return default round num.
     */
    int defaultRoundNum(int l);
}
