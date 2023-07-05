package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * Zl edaBit generation config.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public interface ZlEdaBitGenConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    ZlEdaBitGenFactory.ZlEdaBitGenType getPtoType();

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    Zl getZl();
}
