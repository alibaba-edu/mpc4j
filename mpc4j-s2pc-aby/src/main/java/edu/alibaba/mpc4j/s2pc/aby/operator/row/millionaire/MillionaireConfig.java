package edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Millionaire Protocol Config.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public interface MillionaireConfig extends MultiPartyPtoConfig {
    /**
     * Return type of protocol.
     *
     * @return protocol type.
     */
    MillionaireFactory.MillionaireType getPtoType();
}
