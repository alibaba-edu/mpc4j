package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfFactory.SpCdpprfType;

/**
 * single-point CDPPRF config.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public interface SpCdpprfConfig extends MultiPartyPtoConfig {
    /**
     * Get the type.
     *
     * @return the type.
     */
    SpCdpprfType getPtoType();
}
