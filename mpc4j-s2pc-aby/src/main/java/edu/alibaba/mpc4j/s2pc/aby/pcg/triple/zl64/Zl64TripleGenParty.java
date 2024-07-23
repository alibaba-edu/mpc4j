package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Zl64Triple;

/**
 * Zl64 triple generation party.
 *
 * @author Weiran Liu
 * @date 2024/6/29
 */
public interface Zl64TripleGenParty extends MultiPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxL           max l.
     * @param expectTotalNum expect total num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int expectTotalNum) throws MpcAbortException;

    /**
     * Inits the protocol.
     *
     * @param maxL maxL.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param zl64 Zl64 instance.
     * @param num  num.
     * @return Zl64 triple.
     * @throws MpcAbortException the protocol failure aborts
     */
    Zl64Triple generate(Zl64 zl64, int num) throws MpcAbortException;
}
