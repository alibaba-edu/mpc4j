package edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * Zl Greater Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public interface ZlGreaterConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlGreaterFactory.ZlGreaterType getPtoType();

    /**
     * Get the zl.
     *
     * @return the zl.
     */
    Zl getZl();
}
