package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * Zl multiplication triple generator.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlMtgConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    ZlMtgFactory.ZlMtgType getPtoType();

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    Zl getZl();
}
