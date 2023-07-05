package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl DReLU Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public interface ZlDreluConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlDreluFactory.ZlDreluType getPtoType();
}
