package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;

/**
 * Random OSN config.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public interface RosnConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    RosnType getPtoType();
}
