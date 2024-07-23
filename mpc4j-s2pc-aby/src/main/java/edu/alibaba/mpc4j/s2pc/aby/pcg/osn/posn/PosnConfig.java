package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.PosnFactory.PosnType;

/**
 * OSN protocol configure
 *
 * @author Feng Han
 * @date 2024/05/08
 */
public interface PosnConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    PosnType getPtoType();
}
