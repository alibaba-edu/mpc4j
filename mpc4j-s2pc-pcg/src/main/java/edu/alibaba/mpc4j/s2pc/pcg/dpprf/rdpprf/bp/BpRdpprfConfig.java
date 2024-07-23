package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory.BpRdpprfType;

/**
 * batch-point RDPPRF config.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public interface BpRdpprfConfig extends MultiPartyPtoConfig {
    /**
     * Get the type.
     *
     * @return the type.
     */
    BpRdpprfType getPtoType();
}
