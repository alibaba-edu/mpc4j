package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstFactory.PstType;

/**
 * Partial ST config
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public interface PstConfig extends MultiPartyPtoConfig {
    /**
     * Get the type.
     *
     * @return the type.
     */
    PstType getPtoType();

    /**
     * Get the BST config
     *
     * @return the config.
     */
    BstConfig getBstConfig();
}
