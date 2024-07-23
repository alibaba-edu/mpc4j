//package edu.alibaba.mpc4j.s2pc.aby.operator.row.matMul.zl;
//
//import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
//import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
//import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
//
///**
// * Zl Cross Term Multiplication Party.
// *
// * @author Liqiang Peng
// * @date 2024/6/5
// */
//public interface ZlMatMulParty extends TwoPartyPto {
//    /**
//     * inits the protocol.
//     *
//     * @param maxM   max m.
//     * @param maxN   max n.
//     * @param maxNum max num.
//     * @throws MpcAbortException the protocol failure aborts.
//     */
//    void init(int maxM, int maxN, int maxNum) throws MpcAbortException;
//
//    /**
//     * Executes the protocol.
//     *
//     * @param input input.
//     * @param l     bit length.
//     * @return the party's output.
//     * @throws MpcAbortException the protocol failure aborts.
//     */
//    SquareZlVector crossTerm(SquareZlVector input, int l) throws MpcAbortException;
//}
