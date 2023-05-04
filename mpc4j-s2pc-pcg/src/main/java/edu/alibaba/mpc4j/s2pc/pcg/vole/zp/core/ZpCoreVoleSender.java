package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleSenderOutput;

import java.math.BigInteger;

/**
 * Zp-core VOLE sender.
 *
 * @author Hanwen Feng, Weiran Liu.
 * @date 2022/06/13
 */
public interface ZpCoreVoleSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param zp     the Zp instance.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Zp zp, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param x x。
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    ZpVoleSenderOutput send(BigInteger[] x) throws MpcAbortException;
}
