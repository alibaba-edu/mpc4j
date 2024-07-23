package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.NcLnotFactory.NcLnotType;

/**
 * no-choice 1-out-of-n (with n = 2^l) OT config.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public interface NcLnotConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    NcLnotType getPtoType();

    /**
     * Gets the max num.
     *
     * @param l choice bit length.
     * @return the max num.
     */
    int maxNum(int l);
}
