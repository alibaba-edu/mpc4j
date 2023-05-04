package edu.alibaba.mpc4j.s2pc.opf.psm;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * private set membership config.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public interface PsmConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PsmFactory.PsmType getPtoType();
}
