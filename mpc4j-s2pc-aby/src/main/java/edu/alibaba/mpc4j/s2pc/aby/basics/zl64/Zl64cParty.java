package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

import java.util.Arrays;

/**
 * Zl64 circuit party.
 *
 * @author Li Peng
 * @date 2024/7/23
 */
public interface Zl64cParty extends TwoPartyPto, MpcZl64cParty {
    /**
     * Shares its own vector.
     *
     * @param xi the vector to be shared.
     * @return the shared vector.
     */
    @Override
    SquareZl64Vector shareOwn(Zl64Vector xi);

    /**
     * Shares its own vectors。
     *
     * @param xiArray the vectors to be shared.
     * @return the shared vectors.
     */
    @Override
    default SquareZl64Vector[] shareOwn(Zl64Vector[] xiArray) {
        if (xiArray.length == 0) {
            return new SquareZl64Vector[0];
        }
        // merge
        Zl64Vector mergeX = Zl64Vector.merge(xiArray);
        // share
        SquareZl64Vector mergeShareXi = shareOwn(mergeX);
        // split
        int[] nums = Arrays.stream(xiArray).mapToInt(Zl64Vector::getNum).toArray();
        return Arrays.stream(split(mergeShareXi, nums))
            .map(vector -> (SquareZl64Vector) vector)
            .toArray(SquareZl64Vector[]::new);
    }

    /**
     * Shares other's vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  the num to be shared.
     * @return the shared vector.
     * @throws MpcAbortException the protocol failure aborts.
     */
    @Override
    SquareZl64Vector shareOther(Zl64 zl64, int num) throws MpcAbortException;

    /**
     * Shares other's vectors.
     *
     * @param zl   Zl instance.
     * @param nums nums for each vector to be shared.
     * @return the shared vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    @Override
    default SquareZl64Vector[] shareOther(Zl64 zl64, int[] nums) throws MpcAbortException {
        if (nums.length == 0) {
            return new SquareZl64Vector[0];
        }
        // share
        int totalNum = Arrays.stream(nums).sum();
        SquareZl64Vector mergeShareXi = shareOther(zl64, totalNum);
        // split
        return Arrays.stream(split(mergeShareXi, nums))
            .map(vector -> (SquareZl64Vector) vector)
            .toArray(SquareZl64Vector[]::new);
    }

    /**
     * Reveals its own vectors.
     *
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    @Override
    default Zl64Vector[] revealOwn(MpcZl64Vector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new Zl64Vector[0];
        }
        // merge
        SquareZl64Vector mergeXiArray = (SquareZl64Vector) merge(xiArray);
        // reveal
        Zl64Vector mergeX = revealOwn(mergeXiArray);
        // split
        int[] nums = Arrays.stream(xiArray)
            .map(vector -> (SquareZl64Vector) vector)
            .mapToInt(SquareZl64Vector::getNum).toArray();
        return mergeX.split(nums);
    }

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    @Override
    default void revealOther(MpcZl64Vector[] xiArray) {
        //noinspection StatementWithEmptyBody
        if (xiArray.length == 0) {
            // do nothing for 0 length
        }
        // merge
        SquareZl64Vector mergeXiArray = (SquareZl64Vector) merge(xiArray);
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
    SquareZl64Vector add(MpcZl64Vector xi, MpcZl64Vector yi) throws MpcAbortException;

    /**
     * Vector addition.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] + y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZl64Vector[] add(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) throws MpcAbortException;

    /**
     * Subtraction.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x - y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZl64Vector sub(MpcZl64Vector xi, MpcZl64Vector yi) throws MpcAbortException;

    /**
     * Vector subtraction.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] - y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZl64Vector[] sub(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) throws MpcAbortException;

    /**
     * Negation.
     *
     * @param xi xi.
     * @return zi, such that z = -x.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZl64Vector neg(MpcZl64Vector xi) throws MpcAbortException;

    /**
     * Vector negation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = -x[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZl64Vector[] neg(MpcZl64Vector[] xiArray) throws MpcAbortException;

    /**
     * Multiplication.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x * y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZl64Vector mul(MpcZl64Vector xi, MpcZl64Vector yi) throws MpcAbortException;

    /**
     * Vector multiplication.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = z[i] * y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZl64Vector[] mul(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) throws MpcAbortException;
}
