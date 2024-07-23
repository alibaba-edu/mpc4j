package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.ZlTriple;

/**
 * Zl triple generation party.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public interface ZlTripleGenParty extends MultiPartyPto {
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
     * @param zl  Zl instance.
     * @param num num.
     * @return Zl triple.
     * @throws MpcAbortException the protocol failure aborts
     */
    ZlTriple generate(Zl zl, int num) throws MpcAbortException;
}
