package edu.alibaba.mpc4j.common.circuit.zl64;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

/**
 * MPC Zl64 Circuit Party.
 *
 * @author Weiran Liu
 * @date 2024/6/20
 */
public interface MpcZl64cParty {
    /**
     * Creates a (plain) vector.
     *
     * @param vector the assigned vector.
     * @return a vector.
     */
    MpcZl64Vector create(Zl64Vector vector);

    /**
     * Creates a (plain) all-one vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  num.
     * @return a vector.
     */
    MpcZl64Vector createOnes(Zl64 zl64, int num);

    /**
     * Creates a (plain) all-zero vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  num.
     * @return a vector.
     */
    MpcZl64Vector createZeros(Zl64 zl64, int num);

    /**
     * Creates an empty vector.
     *
     * @param zl64  Zl64 instance.
     * @param plain the plain state.
     * @return a vector.
     */
    MpcZl64Vector createEmpty(Zl64 zl64, boolean plain);

    /**
     * merges the vector.
     *
     * @param vectors vectors.
     * @return the merged vector.
     */
    default MpcZl64Vector merge(MpcZl64Vector[] vectors) {
        assert vectors.length > 0 : "merged vector length must be greater than 0";
        boolean plain = vectors[0].isPlain();
        Zl64 zl64 = vectors[0].getZl64();
        MpcZl64Vector mergeVector = createEmpty(zl64, plain);
        // we must merge the bit vector in the reverse order
        for (MpcZl64Vector vector : vectors) {
            assert vector.getNum() > 0 : "the number of bits must be greater than 0";
            mergeVector.merge(vector);
        }
        return mergeVector;
    }

    /**
     * splits the vector.
     *
     * @param mergeVector the merged vector.
     * @param nums        num for each of the split vector.
     * @return the split vector.
     */
    default MpcZl64Vector[] split(MpcZl64Vector mergeVector, int[] nums) {
        MpcZl64Vector[] splitVectors = new MpcZl64Vector[nums.length];
        for (int index = nums.length - 1; index >= 0; index--) {
            splitVectors[index] = (MpcZl64Vector) mergeVector.split(nums[index]);
        }
        assert mergeVector.getNum() == 0 : "merged vector must remain 0 num: " + mergeVector.getNum();
        return splitVectors;
    }

    /**
     * Inits the protocol.
     *
     * @param maxL           maxL.
     * @param expectTotalNum expect total num.
     * @throws MpcAbortException if the protocol is abort.
     */
    void init(int maxL, int expectTotalNum) throws MpcAbortException;

    /**
     * Inits the protocol.
     *
     * @param maxL maxL.
     * @throws MpcAbortException if the protocol is abort.
     */
    void init(int maxL) throws MpcAbortException;

    /**
     * Shares its own vector.
     *
     * @param xi the vector to be shared.
     * @return the shared vector.
     */
    MpcZl64Vector shareOwn(Zl64Vector xi);

    /**
     * Shares its own vector.
     *
     * @param xiArray the vectors to be shared.
     * @return the shared vectors.
     */
    MpcZl64Vector[] shareOwn(Zl64Vector[] xiArray);

    /**
     * Shares other's vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  num to be shared.
     * @return the shared vector.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector shareOther(Zl64 zl64, int num) throws MpcAbortException;

    /**
     * Share other's BitVectors.
     *
     * @param zl64 Zl64 instance.
     * @param nums nums for each vector to be shared.
     * @return the shared vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector[] shareOther(Zl64 zl64, int[] nums) throws MpcAbortException;

    /**
     * Reveals its own vector.
     *
     * @param xi the shared vector.
     * @return the revealed vector.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Zl64Vector revealOwn(MpcZl64Vector xi) throws MpcAbortException;

    /**
     * Reveals its own vectors.
     *
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Zl64Vector[] revealOwn(MpcZl64Vector[] xiArray) throws MpcAbortException;

    /**
     * Reveals other's vector.
     *
     * @param xi the shared vector.
     */
    void revealOther(MpcZl64Vector xi);

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    void revealOther(MpcZl64Vector[] xiArray);

    /**
     * Add operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x + y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector add(MpcZl64Vector xi, MpcZl64Vector yi) throws MpcAbortException;

    /**
     * Vector add operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] + y[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector[] add(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) throws MpcAbortException;

    /**
     * Sub operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x - y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector sub(MpcZl64Vector xi, MpcZl64Vector yi) throws MpcAbortException;

    /**
     * Vector sub operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] - y[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector[] sub(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) throws MpcAbortException;

    /**
     * Neg operation.
     *
     * @param xi xi.
     * @return zi, such that z = -x.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector neg(MpcZl64Vector xi) throws MpcAbortException;

    /**
     * Vector neg operations.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = -x[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector[] neg(MpcZl64Vector[] xiArray) throws MpcAbortException;

    /**
     * Mul operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x * y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector mul(MpcZl64Vector xi, MpcZl64Vector yi) throws MpcAbortException;

    /**
     * Vector mul operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] * y[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZl64Vector[] mul(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) throws MpcAbortException;
}
