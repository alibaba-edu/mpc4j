package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * @author Feng Han
 * @date 2024/12/9
 */
public interface OoPsuClient extends PsuClient {
    /**
     * Do pre-computation.
     *
     * @param clientElementSize client element size.
     * @param serverElementSize server element size.
     * @param elementByteLength element byte length.
     * @throws MpcAbortException the protocol failure abort.
     */
    void preCompute(int clientElementSize, int serverElementSize, int elementByteLength) throws MpcAbortException;
}