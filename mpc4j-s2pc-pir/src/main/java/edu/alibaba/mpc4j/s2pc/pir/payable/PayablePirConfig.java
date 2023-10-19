package edu.alibaba.mpc4j.s2pc.pir.payable;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public interface PayablePirConfig extends MultiPartyPtoConfig {
    /**
     * protocol type.
     *
     * @return protocol type.
     */
    PayablePirFactory.PayablePirType getProType();
}
