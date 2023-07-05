package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * client-payload circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface CcpsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    CcpsiFactory.CcpsiType getPtoType();

    /**
     * Gets number of shared bits.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return number of shared bits.
     */
    int getOutputBitNum(int serverElementSize, int clientElementSize);
}
