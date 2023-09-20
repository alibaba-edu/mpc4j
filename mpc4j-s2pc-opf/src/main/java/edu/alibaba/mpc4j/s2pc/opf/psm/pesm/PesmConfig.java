package edu.alibaba.mpc4j.s2pc.opf.psm.pesm;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * private (equal) set membership config.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public interface PesmConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PesmFactory.PesmType getPtoType();
}
