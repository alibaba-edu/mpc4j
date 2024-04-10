package edu.alibaba.mpc4j.common.circuit.zlong;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * MPC Zlong Circuit Party.
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public interface MpcLongParty {
    /**
     * get parallel setting
     *
     * @return status
     */
    boolean getParallel();

    /**
     * Creates a vector with assigned value, and specify whether it is shared
     *
     * @param longVector assigned value.
     * @param isPlain    whether this vector is plaintext or not
     * @return a vector.
     */
    MpcLongVector create(boolean isPlain, LongVector... longVector);

    /**
     * Creates a vector with assigned value, and specify whether it is shared
     *
     * @param longs assigned value.
     * @param isPlain    whether this vector is plaintext or not
     * @return a vector.
     */
    MpcLongVector create(boolean isPlain, long[]... longs);

    /**
     * merges the vector.
     *
     * @param vectors vectors.
     * @return the merged vector.
     */
    default MpcLongVector merge(MpcLongVector[] vectors) {
        assert vectors.length > 0 : "merged vector length must be greater than 0";
        boolean plain = vectors[0].isPlain();
        LongVector[] data = IntStream.range(0, vectors[0].getVectors().length).mapToObj(i ->
                LongVector.merge(Arrays.stream(vectors).map(x -> {
                    assert i != 0 || plain == x.isPlain();
                    return x.getVectors()[i];
                }).toArray(LongVector[]::new)))
            .toArray(LongVector[]::new);
        return create(plain, data);
    }

    /**
     * splits the vector.
     *
     * @param mergeVector the merged vector.
     * @param nums        num for each of the split vector.
     * @return the split vector.
     */
    default MpcLongVector[] split(MpcLongVector mergeVector, int[] nums) {
        boolean isPlain = mergeVector.isPlain();
        LongVector[][] splitRes = Arrays.stream(mergeVector.getVectors()).map(x ->
            x.split(nums)).toArray(LongVector[][]::new);
        return IntStream.range(0, nums.length).mapToObj(i ->
                create(isPlain, Arrays.stream(splitRes).map(x -> x[i]).toArray(LongVector[]::new)))
            .toArray(MpcLongVector[]::new);
    }

    /**
     * Shares public vector.
     *
     * @param xi public vector to be shared.
     * @return the shared vector.
     */
    MpcLongVector setPublicValue(LongVector xi);

    /**
     * Shares its own vector.
     *
     * @param xi the vector to be shared.
     * @return the shared vector.
     */
    default MpcLongVector shareOwn(LongVector xi) throws MpcAbortException {
        return shareOwn(new LongVector[]{xi})[0];
    }

    /**
     * Shares its own vector.
     *
     * @param xiArray the vectors to be shared.
     * @return the shared vectors.
     */
    MpcLongVector[] shareOwn(LongVector[] xiArray) throws MpcAbortException;

    /**
     * Shares other's vector.
     *
     * @param num num to be shared.
     * @param party the party that the data belongs to
     * @return the shared vector.
     */
    default MpcLongVector shareOther(int num, Party party) throws MpcAbortException {
        return shareOther(new int[]{num}, party)[0];
    }

    /**
     * Share other's BitVectors.
     *
     * @param nums nums for each vector to be shared.
     * @param party the party that the data belongs to
     * @return the shared vectors.
     */
    MpcLongVector[] shareOther(int[] nums, Party party) throws MpcAbortException;

    /**
     * Reveals its own vectors.
     *
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default LongVector[] revealOwn(MpcLongVector... xiArray) throws MpcAbortException{
        return revealOwn(64, xiArray);
    }

    /**
     * Reveals its own vectors.
     *
     * @param validBitLen valid bit length of opened values
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    LongVector[] revealOwn(int validBitLen, MpcLongVector... xiArray) throws MpcAbortException;

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     * @param party the party that the data belongs to
     */
    void revealOther(Party party, MpcLongVector... xiArray) throws MpcAbortException;

    /**
     * Open the shared values
     *
     * @param xiArray the shared vectors.
     */
    default LongVector[] open(MpcLongVector... xiArray) throws MpcAbortException{
        return open(64, xiArray);
    }

    /**
     * Open the shared values
     *
     * @param validBits valid bit length of opened values
     * @param xiArray the shared vectors.
     */
    LongVector[] open(int validBits, MpcLongVector... xiArray) throws MpcAbortException;

    /**
     * Add operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x + y.
     */
    MpcLongVector add(MpcLongVector xi, MpcLongVector yi);

    /**
     * Vector add operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] + y[i].
     */
    default MpcLongVector[] add(MpcLongVector[] xiArray, MpcLongVector[] yiArray){
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        IntStream intStream = getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i -> add(xiArray[i], yiArray[i])).toArray(MpcLongVector[]::new);
    }

    /**
     * Add operation. x = x + y.
     *
     * @param xi xi.
     * @param yi yi.
     */
    void addi(MpcLongVector xi, MpcLongVector yi);

    /**
     * Vector add operations. x[i] = x[i] + y[i].
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     */
    default void addi(MpcLongVector[] xiArray, MpcLongVector[] yiArray){
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        IntStream intStream = getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> addi(xiArray[i], yiArray[i]));
    }

    /**
     * Sub operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x - y.
     */
    MpcLongVector sub(MpcLongVector xi, MpcLongVector yi);

    /**
     * Vector sub operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] - y[i].
     */
    default MpcLongVector[] sub(MpcLongVector[] xiArray, MpcLongVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        IntStream intStream = getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i -> sub(xiArray[i], yiArray[i])).toArray(MpcLongVector[]::new);
    }

    /**
     * Sub operation. x = x - y.
     *
     * @param xi xi.
     * @param yi yi.
     */
    void subi(MpcLongVector xi, MpcLongVector yi);

    /**
     * Vector sub operations. x[i] = x[i] - y[i].
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     */
    default void subi(MpcLongVector[] xiArray, MpcLongVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        IntStream intStream = getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> subi(xiArray[i], yiArray[i]));
    }

    /**
     * Neg operation.
     *
     * @param xi xi.
     * @return zi, such that z = -x.
     */
    MpcLongVector neg(MpcLongVector xi);

    /**
     * Vector neg operations.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = -x[i].
     */
    default MpcLongVector[] neg(MpcLongVector[] xiArray){
        IntStream intStream = getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i -> neg(xiArray[i])).toArray(MpcLongVector[]::new);
    }

    /**
     * Neg operation. x = -x.
     *
     * @param xi xi.
     */
    void negi(MpcLongVector xi);

    /**
     * Neg operation. x[i] = -x[i].
     *
     * @param xiArray xi array.
     */
    default void negi(MpcLongVector[] xiArray){
        Stream<MpcLongVector> stream = getParallel() ? Arrays.stream(xiArray).parallel() : Arrays.stream(xiArray);
        stream.forEach(this::negi);
    }

    /**
     * Mul operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x * y.
     */
    default MpcLongVector mul(MpcLongVector xi, MpcLongVector yi){
        return mul(new MpcLongVector[]{xi}, new MpcLongVector[]{yi})[0];
    }

    /**
     * Vector mul operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] * y[i].
     */
    MpcLongVector[] mul(MpcLongVector[] xiArray, MpcLongVector[] yiArray);

    /**
     * Mul operation. x = x * y.
     *
     * @param xi xi.
     * @param yi yi.
     */
    default void muli(MpcLongVector xi, PlainLongVector yi){
        muli(new MpcLongVector[]{xi}, new PlainLongVector[]{yi});
    }

    /**
     * Vector mul operation. x[i] = x[i] * y[i].
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     */
    void muli(MpcLongVector[] xiArray, PlainLongVector[] yiArray);
}
