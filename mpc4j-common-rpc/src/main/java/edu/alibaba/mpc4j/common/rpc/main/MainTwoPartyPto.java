package edu.alibaba.mpc4j.common.rpc.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

/**
 * main 2PC protocol.
 *
 * @author Weiran Liu
 * @date 2024/5/3
 */
public interface MainTwoPartyPto {
    /**
     * Runs Netty.
     *
     * @throws IOException       for IOException.
     * @throws MpcAbortException for MPC Abort Exception.
     */
    void runNetty() throws IOException, MpcAbortException;

    /**
     * Runs the first party.
     *
     * @param party1Rpc RPC for Party 1.
     * @param party2    Party 2.
     * @throws IOException       for IOException.
     * @throws MpcAbortException for MPC Abort Exception.
     */
    void runParty1(Rpc party1Rpc, Party party2) throws IOException, MpcAbortException;

    /**
     * Runs the second party.
     *
     * @param party2Rpc RPC for Party 2.
     * @param party1    Party 1.
     * @throws IOException       for IOException.
     * @throws MpcAbortException for MPC Abort Exception.
     */
    void runParty2(Rpc party2Rpc, Party party1) throws IOException, MpcAbortException;
}
