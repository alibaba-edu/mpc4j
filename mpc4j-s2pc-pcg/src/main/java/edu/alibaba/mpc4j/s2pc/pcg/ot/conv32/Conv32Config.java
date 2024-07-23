package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;

/**
 * F_3 -> F_2 modulus conversion config.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
public interface Conv32Config extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    Conv32Type getPtoType();
}
