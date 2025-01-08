package edu.alibaba.mpc4j.s2pc.opf.osorter;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.osorter.ObSortFactory.ObSortType;

/**
 * @author Feng Han
 * @date 2024/9/27
 */
public interface ObSortConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ObSortType getPtoType();
}
