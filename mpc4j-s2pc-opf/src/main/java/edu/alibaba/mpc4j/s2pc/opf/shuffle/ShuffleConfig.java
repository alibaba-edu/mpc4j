package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleType;

/**
 * shuffle configure interface
 *
 * @author Feng Han
 * @date 2024/9/26
 */
public interface ShuffleConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ShuffleType getPtoType();

    /**
     * Gets the type of permutation.
     *
     * @return RosnType.
     */
    RosnType getRosnType();
}