package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * private (distinct) set membership config.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public interface PdsmConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PdsmFactory.PdsmType getPtoType();
}
