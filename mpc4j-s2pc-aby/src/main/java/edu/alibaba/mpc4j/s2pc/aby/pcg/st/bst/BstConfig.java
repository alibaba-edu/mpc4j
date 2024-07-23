package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory.BstType;

/**
 * Batched Share Translation config.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
public interface BstConfig extends MultiPartyPtoConfig {
    /**
     * Get the type.
     *
     * @return the type.
     */
    BstType getPtoType();
}
