package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.circuit.z2.MpcBcParty;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Boolean circuit party.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface BcParty extends TwoPartyPto, MpcBcParty {
    /**
     * init the protocol.
     *
     * @param maxRoundBitNum maximum number of bits in round.
     * @param updateBitNum   total number of bits for updates.
     * @throws MpcAbortException if the protocol is abort.
     */
    void init(int maxRoundBitNum, int updateBitNum) throws MpcAbortException;

    /**
     * Share its own BitVector.
     *
     * @param x the BitVector to be shared.
     * @return the shared BitVector.
     */
    SquareZ2Vector shareOwn(BitVector x);

    /**
     * Share its own BitVectors。
     *
     * @param xArray the BitVectors to be shared.
     * @return the shared BitVectors.
     */
    SquareZ2Vector[] shareOwn(BitVector[] xArray);

    /**
     * Share other's BitVector.
     *
     * @param bitNum the number of bits to be shared.
     * @return the shared BitVector.
     * @throws MpcAbortException if the protocol is abort.
     */
    SquareZ2Vector shareOther(int bitNum) throws MpcAbortException;

    /**
     * Share other's BitVectors.
     *
     * @param bitNums the number of bits for each bit vector to be shared.
     * @return the shared BitVectors.
     * @throws MpcAbortException if the protocol is abort.
     */
    SquareZ2Vector[] shareOther(int[] bitNums) throws MpcAbortException;

    /**
     * Reveal its own BitVector.
     *
     * @param xi the shared BitVector.
     * @return the reconstructed BitVector.
     * @throws MpcAbortException if the protocol is abort.
     */
    BitVector revealOwn(SquareZ2Vector xi) throws MpcAbortException;

    /**
     * Reveal its own BitVectors.
     *
     * @param xiArray the shared BitVectors.
     * @return the reconstructed BitVectors.
     * @throws MpcAbortException if the protocol is abort.
     */
    BitVector[] revealOwn(SquareZ2Vector[] xiArray) throws MpcAbortException;

    /**
     * Reconstruct other's BitVector.
     *
     * @param xi the shared BitVector.
     */
    void revealOther(SquareZ2Vector xi);

    /**
     * Reconstruct other's BitVectors.
     *
     * @param xiArray the shared BitVectors.
     */
    void revealOther(SquareZ2Vector[] xiArray);

    /**
     * AND operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x & y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector AND operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] & y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * XOR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x ^ y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector XOR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] ^ y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * NOT operation.
     *
     * @param xi xi.
     * @return zi, such that z = !x.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector not(MpcZ2Vector xi) throws MpcAbortException;

    /**
     * Vector NOT operation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = !x[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector[] not(MpcZ2Vector[] xiArray) throws MpcAbortException;

    /**
     * OR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x | y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default SquareZ2Vector or(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException {
        return xor(xor(xi, yi), and(xi, yi));
    }

    /**
     * Vector OR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = z[i] | y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default SquareZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        return xor(xor(xiArray, yiArray), and(xiArray, yiArray));
    }
}
