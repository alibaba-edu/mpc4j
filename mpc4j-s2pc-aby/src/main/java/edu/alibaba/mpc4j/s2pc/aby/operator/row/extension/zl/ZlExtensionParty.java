package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl Value Extension Party.
 *
 * @author Liqiang Peng
 * @date 2024/5/29
 */
public interface ZlExtensionParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxInputL  max input l.
     * @param maxOutputL max output l.
     * @param maxNum     max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxInputL, int maxOutputL, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xi       xi.
     * @param outputL  output l.
     * @param inputMsb input most significant bit.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZlVector zExtend(SquareZlVector xi, int outputL, boolean inputMsb) throws MpcAbortException;
}
