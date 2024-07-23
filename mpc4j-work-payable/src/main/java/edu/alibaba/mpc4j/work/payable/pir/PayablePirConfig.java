package edu.alibaba.mpc4j.work.payable.pir;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Payable PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public interface PayablePirConfig extends MultiPartyPtoConfig {
    /**
     * protocol type.
     *
     * @return protocol type.
     */
    PayablePirFactory.PayablePirType getProType();
}
