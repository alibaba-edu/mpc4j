package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory.BpCdpprfType;

/**
 * BP-CDPPRF config.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public interface BpCdpprfConfig extends MultiPartyPtoConfig {
    /**
     * Get the type.
     *
     * @return the type.
     */
    BpCdpprfType getPtoType();
}
