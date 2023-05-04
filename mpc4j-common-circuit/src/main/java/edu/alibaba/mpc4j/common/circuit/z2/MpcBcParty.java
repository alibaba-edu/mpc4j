package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Mpc Boolean Circuit Party.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public interface MpcBcParty {
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
    MpcZ2Vector create(int bitNum, boolean value);

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
            assert vector.getNum() > 0 : "the number of bits must be greater than 0";
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
        assert mergeVector.getNum() == 0 : "merged vector must remain 0 bits: " + mergeVector.getNum();
        return splitVectors;
    }

    /**
     * AND operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x & y.
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector AND operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] & y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * XOR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x ^ y.
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector XOR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] ^ y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * OR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x | y.
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector or(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector OR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = z[i] | y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * NOT operation.
     *
     * @param xi xi.
     * @return zi, such that z = !x.
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector not(MpcZ2Vector xi) throws MpcAbortException;

    /**
     * Vector NOT operation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = !x[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] not(MpcZ2Vector[] xiArray) throws MpcAbortException;
}
