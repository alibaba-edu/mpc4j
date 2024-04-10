package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;

/**
 * Interface for three-party shuffling
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public interface ShuffleParty extends ThreePartyPto {

    /**
     * get the required number of tuples according to operation
     *
     * @param op           the required operation
     * @param inputDataNum  size of input data in each dimension
     * @param outputDataNum size of output data in each dimension
     * @param dataDim       input data dimension
     */
    long getTupleNum(ShuffleOp op, int inputDataNum, int outputDataNum, int dataDim);

    /**
     * initialize the party
     */
    void init();

    /**
     * shuffle the data with predetermined permutation, the elements are stored in row
     *
     * @param data data to be shuffled
     * @param pai  predetermined permutation
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] shuffleRow(int[][] pai, MpcZ2Vector[] data) throws MpcAbortException;

    /**
     * randomly shuffle the data, the elements are stored in row
     *
     * @param data data to be shuffled
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] shuffleRow(MpcZ2Vector[] data) throws MpcAbortException;

    /**
     * shuffle the data with predetermined permutation, the elements are stored in column
     *
     * @param data data to be shuffled
     * @param pai  predetermined permutation
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] shuffleColumn(int[][] pai, MpcZ2Vector... data) throws MpcAbortException;

    /**
     * randomly shuffle the data, the elements are stored in column
     *
     * @param data data to be shuffled
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] shuffleColumn(MpcZ2Vector... data) throws MpcAbortException;

    /**
     * shuffle the data with predetermined permutation, whose result is Y = pai · X
     *
     * @param data data to be shuffled
     * @param pai  predetermined permutation
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcLongVector[] shuffle(int[][] pai, MpcLongVector... data) throws MpcAbortException;

    /**
     * shuffle the data with predetermined permutation, whose result is Y = pai · X
     * then open the result to all parties
     *
     * @param data data to be shuffled
     * @param pai  predetermined permutation
     * @throws MpcAbortException if the protocol is abort.
     */
    LongVector[] shuffleOpen(int[][] pai, MpcLongVector... data) throws MpcAbortException;

    /**
     * randomly shuffle the data
     *
     * @param data data to be shuffled
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcLongVector[] shuffle(MpcLongVector... data) throws MpcAbortException;

    /**
     * shuffle the data with predetermined permutation in the inverse order, whose result is Y = pai^{-1} · X
     *
     * @param data data to be shuffled
     * @param pai  predetermined permutation
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcLongVector[] invShuffle(int[][] pai, MpcLongVector... data) throws MpcAbortException;

    /**
     * switch the data with predetermined function whose result is Y_i = X_fun[i]
     *
     * @param input      data to be shuffled
     * @param fun        predetermined function
     * @param targetLen  the result length
     * @param programmer who is the programmer
     * @param sender     who is the sender
     * @param receiver   who is the receiver
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] switchNetwork(MpcZ2Vector[] input, int[] fun, int targetLen, Party programmer, Party sender, Party receiver) throws MpcAbortException;

    /**
     * permute the data with predetermined function whose result is Y_i = X_fun[i]
     *
     * @param input      data to be shuffled
     * @param fun        predetermined function
     * @param targetLen  the result length
     * @param programmer who is the programmer
     * @param sender     who is the sender
     * @param receiver   who is the receiver
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] permuteNetwork(MpcZ2Vector[] input, int[] fun, int targetLen, Party programmer, Party sender, Party receiver) throws MpcAbortException;

    /**
     * duplicate the data with predetermined function, data is in the form of column
     *
     * @param input      input data
     * @param flag       if flag[i] = true, then Y_i = Y_{i-1}; otherwise, Y_i = X_{i}
     * @param programmer who is the programmer
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] duplicateNetwork(MpcZ2Vector[] input, boolean[] flag, Party programmer) throws MpcAbortException;

    /**
     * switch the data with predetermined function whose result is Y_i = X_fun[i], data is in the form of column
     *
     * @param input      data to be shuffled
     * @param fun        predetermined function
     * @param targetLen  the result length
     * @param programmer who is the programmer
     * @param sender     who is the sender
     * @param receiver   who is the receiver
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcLongVector[] switchNetwork(MpcLongVector[] input, int[] fun, int targetLen, Party programmer, Party sender, Party receiver) throws MpcAbortException;

    /**
     * permute the data with predetermined function whose result is Y_i = X_fun[i], data is in the form of column
     *
     * @param input      data to be shuffled
     * @param fun        predetermined function
     * @param targetLen  the result length
     * @param programmer who is the programmer
     * @param sender     who is the sender
     * @param receiver   who is the receiver
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcLongVector[] permuteNetwork(MpcLongVector[] input, int[] fun, int targetLen, Party programmer, Party sender, Party receiver) throws MpcAbortException;

    /**
     * duplicate the data with predetermined function, data is in the form of column
     *
     * @param input      input data
     * @param flag       if flag[i] = true, then Y_i = Y_{i-1}; otherwise, Y_i = X_{i}
     * @param programmer who is the programmer
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcLongVector[] duplicateNetwork(MpcLongVector[] input, boolean[] flag, Party programmer) throws MpcAbortException;
}
