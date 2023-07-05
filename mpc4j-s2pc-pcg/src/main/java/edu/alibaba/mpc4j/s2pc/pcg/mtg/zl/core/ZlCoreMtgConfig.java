package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * Zl core multiplication config.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlCoreMtgConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    ZlCoreMtgFactory.ZlCoreMtgType getPtoType();

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    Zl getZl();

    /**
     * Gets max num.
     *
     * @return max num.
     */
    int maxNum();
}
