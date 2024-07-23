package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.ZlDaBitGenFactory.ZlDaBitGenType;

/**
 * Zl daBit generation config.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public interface ZlDaBitGenConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    ZlDaBitGenType getPtoType();
}
