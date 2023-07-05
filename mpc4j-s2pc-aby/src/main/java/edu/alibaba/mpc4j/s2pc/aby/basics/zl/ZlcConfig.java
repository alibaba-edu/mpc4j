package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

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
    ZlcFactory.ZlType getPtoType();

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    Zl getZl();
}
