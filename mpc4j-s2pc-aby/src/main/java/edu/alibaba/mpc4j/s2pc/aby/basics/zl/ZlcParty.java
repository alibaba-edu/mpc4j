package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.zl.MpcZlParty;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

import java.util.Arrays;

/**
 * Zl circuit party.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public interface ZlcParty extends TwoPartyPto, MpcZlParty {
    /**
     * Shares its own vector.
     *
     * @param xi the vector to be shared.
     * @return the shared vector.
     */
    @Override
    SquareZlVector shareOwn(ZlVector xi);

    /**
     * Shares its own vectors。
     *
     * @param xiArray the vectors to be shared.
     * @return the shared vectors.
     */
    @Override
    default SquareZlVector[] shareOwn(ZlVector[] xiArray) {
        if (xiArray.length == 0) {
            return new SquareZlVector[0];
        }
        // merge
        ZlVector mergeX = ZlVector.merge(xiArray);
        // share
        SquareZlVector mergeShareXi = shareOwn(mergeX);
        // split
        int[] nums = Arrays.stream(xiArray).mapToInt(ZlVector::getNum).toArray();
        return Arrays.stream(split(mergeShareXi, nums))
            .map(vector -> (SquareZlVector) vector)
            .toArray(SquareZlVector[]::new);
    }

    /**
     * Shares other's vector.
     *
     * @param num the num to be shared.
     * @return the shared vector.
     * @throws MpcAbortException the protocol failure aborts.
     */
    @Override
    SquareZlVector shareOther(int num) throws MpcAbortException;

    /**
     * Shares other's vectors.
     *
     * @param nums nums for each vector to be shared.
     * @return the shared vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    @Override
    default SquareZlVector[] shareOther(int[] nums) throws MpcAbortException {
        if (nums.length == 0) {
            return new SquareZlVector[0];
        }
        // share
        int totalNum = Arrays.stream(nums).sum();
        SquareZlVector mergeShareXi = shareOther(totalNum);
        // split
        return Arrays.stream(split(mergeShareXi, nums))
            .map(vector -> (SquareZlVector) vector)
            .toArray(SquareZlVector[]::new);
    }

    /**
     * Reveals its own vectors.
     *
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    @Override
    default ZlVector[] revealOwn(MpcZlVector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new ZlVector[0];
        }
        // merge
        SquareZlVector mergeXiArray = (SquareZlVector) merge(xiArray);
        // reveal
        ZlVector mergeX = revealOwn(mergeXiArray);
        // split
        int[] nums = Arrays.stream(xiArray)
            .map(vector -> (SquareZlVector) vector)
            .mapToInt(SquareZlVector::getNum).toArray();
        return mergeX.split(nums);
    }

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    @Override
    default void revealOther(MpcZlVector[] xiArray) {
        //noinspection StatementWithEmptyBody
        if (xiArray.length == 0) {
            // do nothing for 0 length
        }
        // merge
        SquareZlVector mergeXiArray = (SquareZlVector) merge(xiArray);
        // reveal
        revealOther(mergeXiArray);
    }

    /**
     * Addition.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x + y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector add(MpcZlVector xi, MpcZlVector yi) throws MpcAbortException;

    /**
     * Vector addition.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] + y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector[] add(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException;

    /**
     * Subtraction.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x - y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector sub(MpcZlVector xi, MpcZlVector yi) throws MpcAbortException;

    /**
     * Vector subtraction.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] - y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector[] sub(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException;

    /**
     * Negation.
     *
     * @param xi xi.
     * @return zi, such that z = -x.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector neg(MpcZlVector xi) throws MpcAbortException;

    /**
     * Vector negation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = -x[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector[] neg(MpcZlVector[] xiArray) throws MpcAbortException;

    /**
     * Multiplication.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x * y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector mul(MpcZlVector xi, MpcZlVector yi) throws MpcAbortException;

    /**
     * Vector multiplication.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = z[i] * y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector[] mul(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException;
}
