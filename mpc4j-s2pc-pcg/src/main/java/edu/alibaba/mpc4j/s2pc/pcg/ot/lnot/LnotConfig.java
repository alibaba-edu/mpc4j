package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * 1-out-of-n (with n = 2^l) OT config.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public interface LnotConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    LnotFactory.LnotType getPtoType();

    /**
     * Gets the maximal base num.
     *
     * @return the maximal base num.
     */
    int maxBaseNum();
}
