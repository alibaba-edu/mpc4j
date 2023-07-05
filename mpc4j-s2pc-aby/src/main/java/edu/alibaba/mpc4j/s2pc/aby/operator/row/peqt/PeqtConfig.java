package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * private equality test config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public interface PeqtConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PeqtFactory.PeqtType getPtoType();
}
