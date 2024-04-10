package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * MPC Z2 Circuit Party.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public interface MpcZ2cParty {
    /**
     * get parallel setting
     *
     * @return status
     */
    boolean getParallel();

    /**
     * Creates a vector with assigned value, and specify whether it is shared
     *
     * @param bitVector assigned value.
     * @param isPlain   whether this vector is plaintext or not
     * @return a vector.
     */
    MpcZ2Vector create(boolean isPlain, BitVector... bitVector);

    /**
     * Creates a (plain) all-one vector.
     *
     * @param bitNum the bit num.
     * @return a vector.
     */
    MpcZ2Vector createOnes(int bitNum);

    /**
     * Creates a (plain) all-zero vector.
     *
     * @param bitNum the bit num.
     * @return a vector.
     */
    MpcZ2Vector createZeros(int bitNum);

    /**
     * Creates a (plain) vector with all bits equal to the assigned value.
     *
     * @param bitNum the bit num.
     * @param value  the assigned value.
     * @return a vector.
     */
    default MpcZ2Vector create(int bitNum, boolean value) {
        if (value) {
            return createOnes(bitNum);
        } else {
            return createZeros(bitNum);
        }
    }

    /**
     * Creates an empty vector.
     *
     * @param plain the plain state.
     * @return a vector.
     */
    MpcZ2Vector createEmpty(boolean plain);

    /**
     * merges the vector.
     *
     * @param vectors vectors.
     * @return the merged vector.
     */
    default MpcZ2Vector merge(MpcZ2Vector[] vectors) {
        assert vectors.length > 0 : "merged vector length must be greater than 0";
        boolean plain = vectors[0].isPlain();
        MpcZ2Vector mergeVector = createEmpty(plain);
        // we must merge the bit vector in the reverse order
        for (MpcZ2Vector vector : vectors) {
            assert vector.getNum() > 0 : "num must be greater than 0";
            mergeVector.merge(vector);
        }
        return mergeVector;
    }

    /**
     * splits the vector.
     *
     * @param mergeVector the merged vector.
     * @param bitNums     bits for each of the split vector.
     * @return the split vector.
     */
    default MpcZ2Vector[] split(MpcZ2Vector mergeVector, int[] bitNums) {
        MpcZ2Vector[] splitVectors = new MpcZ2Vector[bitNums.length];
        for (int index = 0; index < bitNums.length; index++) {
            splitVectors[index] = (MpcZ2Vector) mergeVector.split(bitNums[index]);
        }
        assert mergeVector.getNum() == 0 : "merged vector must remain 0 num: " + mergeVector.getNum();
        return splitVectors;
    }

    /**
     * inits the protocol.
     *
     * @param updateBitNum total number of bits for updates.
     * @throws MpcAbortException if the protocol is abort.
     */
    void init(long updateBitNum) throws MpcAbortException;

    /**
     * Shares its own vector.
     *
     * @param xi the vector to be shared.
     * @return the shared vector.
     */
    MpcZ2Vector shareOwn(BitVector xi) throws MpcAbortException;

    /**
     * Shares its own vectors。
     *
     * @param xiArray the vectors to be shared.
     * @return the shared vectors.
     */
    MpcZ2Vector[] shareOwn(BitVector[] xiArray) throws MpcAbortException;

    /**
     * Shares other's vector.
     *
     * @param bitNum the number of bits to be shared.
     * @return the shared vector.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector shareOther(int bitNum) throws MpcAbortException;

    /**
     * Share other's vectors.
     *
     * @param bitNums the number of bits for each vector to be shared.
     * @return the shared vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] shareOther(int[] bitNums) throws MpcAbortException;

    /**
     * Open the vector to all computing parties
     *
     * @param xiArray the data to be opened
     * @throws MpcAbortException the protocol failure aborts.
     */
    BitVector[] open(MpcZ2Vector[] xiArray) throws MpcAbortException;

    /**
     * Reveals its own vector.
     *
     * @param xi the shared vector.
     * @return the revealed vector.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BitVector revealOwn(MpcZ2Vector xi) throws MpcAbortException;

    /**
     * Reveals its own vectors.
     *
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BitVector[] revealOwn(MpcZ2Vector[] xiArray) throws MpcAbortException;

    /**
     * Reveals other's vector.
     *
     * @param xi the shared vector.
     */
    void revealOther(MpcZ2Vector xi);

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    void revealOther(MpcZ2Vector[] xiArray);

    /**
     * AND operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x & y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector AND operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] & y[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * XOR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x ^ y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * XOR operation. the result stored in xi
     *
     * @param xi xi.
     * @param yi yi.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void xori(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector XOR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] ^ y[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * XOR operation. the result stored in xi
     *
     * @param xi xi.
     * @param yi yi.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default void xori(MpcZ2Vector[] xi, MpcZ2Vector[] yi) throws MpcAbortException{
        MathPreconditions.checkEqual("xi.length", "yi.length", xi.length, yi.length);
        for(int i = 0; i < xi.length; i++){
            xori(xi[i], yi[i]);
        }
    }

    /**
     * OR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x | y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector or(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector OR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = z[i] | y[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * NOT operation.
     *
     * @param xi xi.
     * @return zi, such that z = !x.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector not(MpcZ2Vector xi) throws MpcAbortException;

    /**
     * NOT operation.
     *
     * @param xi xi.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void noti(MpcZ2Vector xi) throws MpcAbortException;

    /**
     * Vector NOT operation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = !x[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] not(MpcZ2Vector[] xiArray) throws MpcAbortException;

    /**
     * MUX operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @param ci ci.
     * @return zi, such that z = (c ? y : x).
     * @throws MpcAbortException the protocol failure aborts.
     */
    default MpcZ2Vector mux(MpcZ2Vector xi, MpcZ2Vector yi, MpcZ2Vector ci) throws MpcAbortException {
        return xor(and(xor(xi, yi), ci), xi);
    }

    /**
     * Vector MUX operation.
     *
     * @param xiArray xiArray array.
     * @param yiArray yiArray array.
     * @param ciArray ciArray.
     * @return ziArray, such that for each i, z[i] = (c[i] ? y[i] : x[i]).
     * @throws MpcAbortException the protocol failure aborts.
     */
    default MpcZ2Vector[] mux(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, MpcZ2Vector[] ciArray) throws MpcAbortException {
        return xor(and(xor(xiArray, yiArray), ciArray), xiArray);
    }

    /**
     * Equality operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = (x == y).
     * @throws MpcAbortException the protocol failure aborts.
     */
    default MpcZ2Vector eq(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException {
        return not(xor(xi, yi));
    }

    /**
     * Vector equality operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each i, z[i] = (x[i] == y[i]).
     * @throws MpcAbortException the protocol failure aborts.
     */
    default MpcZ2Vector[] eq(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        return not(xor(xiArray, yiArray));
    }

    /**
     * set public values into shared value
     *
     * @param data the vectors.
     * @return the shared vector.
     */
    MpcZ2Vector[] setPublicValues(BitVector[] data);

    /**
     * splits the vector with padding each vector into a vec with bit length % 8 = 0
     *
     * @param vectors the vectors.
     * @return the merged vector.
     */
    default MpcZ2Vector mergeWithPadding(MpcZ2Vector[] vectors) {
        assert vectors.length > 0 : "merged vector length must be greater than 0";
        boolean plain = vectors[0].isPlain();
        BitVector[][] all = Arrays.stream(vectors).map(x -> {
            assert x.isPlain() == plain;
            return x.getBitVectors();
        }).toArray(BitVector[][]::new);
        BitVector[] mergeBits = IntStream.range(0, all[0].length).mapToObj(i ->
            BitVectorFactory.mergeWithPadding(Arrays.stream(all).map(x -> x[i]).toArray(BitVector[]::new))
        ).toArray(BitVector[]::new);
        return create(plain, mergeBits);
    }
}
