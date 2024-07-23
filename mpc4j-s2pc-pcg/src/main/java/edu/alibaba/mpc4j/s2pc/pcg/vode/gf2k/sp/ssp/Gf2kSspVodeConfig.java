package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeFactory.Gf2kSspVodeType;

/**
 * GF2K-SSP-VODE config.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public interface Gf2kSspVodeConfig extends MultiPartyPtoConfig {
    /**
     * Gets type.
     *
     * @return type.
     */
    Gf2kSspVodeType getPtoType();
}
