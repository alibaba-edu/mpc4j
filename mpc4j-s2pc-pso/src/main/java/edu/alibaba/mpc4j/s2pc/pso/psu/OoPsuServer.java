package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Offline/Online PSO server.
 *
 * @author Feng Han
 * @date 2024/12/9
 */
public interface OoPsuServer extends PsuServer {
    /**
     * Do pre-computation.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @param elementByteLength element byte length.
     * @throws MpcAbortException the protocol failure abort.
     */
    void preCompute(int serverElementSize, int clientElementSize, int elementByteLength) throws MpcAbortException;
}
