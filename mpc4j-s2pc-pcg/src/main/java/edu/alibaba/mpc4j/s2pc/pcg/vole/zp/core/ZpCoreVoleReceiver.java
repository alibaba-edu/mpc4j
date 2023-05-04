package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleReceiverOutput;

import java.math.BigInteger;

/**
 * Zp-core VOLE receiver.
 *
 * @author Hanwen Feng, Weiran Liu.
 * @date 2022/06/08
 */
public interface ZpCoreVoleReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param zp     the Zp instance.
     * @param delta  Δ.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Zp zp, BigInteger delta, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    ZpVoleReceiverOutput receive(int num) throws MpcAbortException;
}
