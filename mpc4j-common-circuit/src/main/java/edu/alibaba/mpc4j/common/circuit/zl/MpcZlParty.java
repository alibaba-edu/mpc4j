package edu.alibaba.mpc4j.common.circuit.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

/**
 * MPC Zl Circuit Party.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public interface MpcZlParty {
    /**
     * Gets the Zl instance.
     *
     * @return the Zl instance.
     */
    Zl getZl();

    /**
     * Creates a (plain) vector with the assigned value.
     *
     * @param zlVector the assigned value.
     * @return a vector.
     */
    MpcZlVector create(ZlVector zlVector);

    /**
     * Creates a (plain) all-one vector.
     *
     * @param num num.
     * @return a vector.
     */
    MpcZlVector createOnes(int num);

    /**
     * Creates a (plain) all-zero Zl vector.
     *
     * @param num num.
     * @return a vector.
     */
    MpcZlVector createZeros(int num);

    /**
     * Creates an empty vector.
     *
     * @param plain the plain state.
     * @return a vector.
     */
    MpcZlVector createEmpty(boolean plain);

    /**
     * merges the vector.
     *
     * @param vectors vectors.
     * @return the merged vector.
     */
    default MpcZlVector merge(MpcZlVector[] vectors) {
        assert vectors.length > 0 : "merged vector length must be greater than 0";
        boolean plain = vectors[0].isPlain();
        MpcZlVector mergeVector = createEmpty(plain);
        // we must merge the bit vector in the reverse order
        for (MpcZlVector vector : vectors) {
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
    default MpcZlVector[] split(MpcZlVector mergeVector, int[] nums) {
        MpcZlVector[] splitVectors = new MpcZlVector[nums.length];
        for (int index = 0; index < nums.length; index++) {
            splitVectors[index] = (MpcZlVector) mergeVector.split(nums[index]);
        }
        assert mergeVector.getNum() == 0 : "merged vector must remain 0 num: " + mergeVector.getNum();
        return splitVectors;
    }

    /**
     * inits the protocol.
     *
     * @param updateNum total num for updates.
     * @throws MpcAbortException if the protocol is abort.
     */
    void init(int updateNum) throws MpcAbortException;

    /**
     * Shares its own vector.
     *
     * @param xi the vector to be shared.
     * @return the shared vector.
     */
    MpcZlVector shareOwn(ZlVector xi);

    /**
     * Shares its own vector.
     *
     * @param xiArray the vectors to be shared.
     * @return the shared vectors.
     */
    MpcZlVector[] shareOwn(ZlVector[] xiArray);

    /**
     * Shares other's vector.
     *
     * @param num num to be shared.
     * @return the shared vector.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector shareOther(int num) throws MpcAbortException;

    /**
     * Share other's BitVectors.
     *
     * @param nums nums for each vector to be shared.
     * @return the shared vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector[] shareOther(int[] nums) throws MpcAbortException;

    /**
     * Reveals its own vector.
     *
     * @param xi the shared vector.
     * @return the revealed vector.
     * @throws MpcAbortException the protocol failure aborts.
     */
    ZlVector revealOwn(MpcZlVector xi) throws MpcAbortException;

    /**
     * Reveals its own vectors.
     *
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    ZlVector[] revealOwn(MpcZlVector[] xiArray) throws MpcAbortException;

    /**
     * Reveals other's vector.
     *
     * @param xi the shared vector.
     */
    void revealOther(MpcZlVector xi);

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    void revealOther(MpcZlVector[] xiArray);

    /**
     * Add operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x + y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector add(MpcZlVector xi, MpcZlVector yi) throws MpcAbortException;

    /**
     * Vector add operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] + y[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector[] add(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException;

    /**
     * Sub operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x - y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector sub(MpcZlVector xi, MpcZlVector yi) throws MpcAbortException;

    /**
     * Vector sub operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] - y[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector[] sub(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException;

    /**
     * Neg operation.
     *
     * @param xi xi.
     * @return zi, such that z = -x.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector neg(MpcZlVector xi) throws MpcAbortException;

    /**
     * Vector neg operations.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = -x[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector[] neg(MpcZlVector[] xiArray) throws MpcAbortException;

    /**
     * Mul operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x * y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector mul(MpcZlVector xi, MpcZlVector yi) throws MpcAbortException;

    /**
     * Vector mul operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] * y[i].
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZlVector[] mul(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException;
}
