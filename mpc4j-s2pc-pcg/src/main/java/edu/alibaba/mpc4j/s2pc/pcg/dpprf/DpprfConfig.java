package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * DPPRF config interface.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public interface DpprfConfig extends SecurePtoConfig {
    /**
     * Get the protocol type.
     *
     * @return the protocol type.
     */
    DpprfFactory.DpprfType getPtoType();
}
