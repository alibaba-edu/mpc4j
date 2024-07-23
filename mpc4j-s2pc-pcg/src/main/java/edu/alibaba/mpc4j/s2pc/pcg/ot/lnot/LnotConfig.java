package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory.LnotType;

/**
 * 1-out-of-n (with n = 2^l) OT config.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public interface LnotConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    LnotType getPtoType();

    /**
     * Gets default round num.
     *
     * @param l l.
     * @return default round num.
     */
    int defaultRoundNum(int l);
}
