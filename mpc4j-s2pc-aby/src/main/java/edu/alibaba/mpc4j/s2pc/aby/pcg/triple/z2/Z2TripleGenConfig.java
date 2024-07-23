package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory.Z2TripleGenType;

/**
 * Z2 triple generation config.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
public interface Z2TripleGenConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    Z2TripleGenType getPtoType();

    /**
     * Gets default round num.
     *
     * @return default round num.
     */
    int defaultRoundNum();
}
