package edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.ZlMax2Factory.ZlMax2Type;

/**
 * Zl Max2 Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public interface ZlMax2Config extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlMax2Type getPtoType();
}
