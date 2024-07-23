package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstFactory.SstType;

/**
 * Single Share Translation config.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public interface SstConfig extends MultiPartyPtoConfig {
    /**
     * Get the type.
     *
     * @return the type.
     */
    SstType getPtoType();
}
