package edu.alibaba.mpc4j.s3pc.abb3.mainpto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

/**
 * main 2PC protocol.
 *
 * @author Weiran Liu
 * @date 2024/5/3
 */
public interface MainAbb3PartyPto {
    /**
     * Runs Netty.
     *
     * @throws IOException       for IOException.
     * @throws MpcAbortException for MPC Abort Exception.
     */
    void runNetty() throws IOException, MpcAbortException;

    /**
     * Runs the specific party
     *
     * @param ownRpc the rpc
     * @throws IOException       for IOException.
     * @throws MpcAbortException for MPC Abort Exception.
     */
    void runParty(Rpc ownRpc) throws IOException, MpcAbortException;
}
