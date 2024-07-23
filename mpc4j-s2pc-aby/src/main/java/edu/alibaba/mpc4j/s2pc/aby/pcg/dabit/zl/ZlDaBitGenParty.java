package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.ZlDaBitTuple;

/**
 * Zl daBit generation party.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public interface ZlDaBitGenParty extends TwoPartyPto {
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
     * @param num number of generated daBit.
     * @return daBit vector.
     * @throws MpcAbortException the protocol if failure abort.
     */
    ZlDaBitTuple generate(Zl zl, int num) throws MpcAbortException;
}
