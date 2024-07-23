package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory.SpRdpprfType;

/**
 * single-point RDPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface SpRdpprfConfig extends MultiPartyPtoConfig {
    /**
     * Get the type.
     *
     * @return the type.
     */
    SpRdpprfType getPtoType();
}
