package edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.ZlMin2Factory.ZlMin2Type;

/**
 * Zl Min2 Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public interface ZlMin2Config extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlMin2Type getPtoType();
}
