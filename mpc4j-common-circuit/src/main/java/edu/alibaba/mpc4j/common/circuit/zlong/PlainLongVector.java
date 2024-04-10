package edu.alibaba.mpc4j.common.circuit.zlong;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * plain Zlong vector.
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class PlainLongVector implements MpcLongVector {
    /**
     * Create a plain Long vector with the assigned value.
     *
     * @param values the assigned values.
     * @return a plain Long vector.
     */
    public static PlainLongVector create(long[] values) {
        return new PlainLongVector(LongVector.create(values));
    }

    /**
     * Creates a plain Long vector with the assigned Long vector.
     *
     * @param longVector the assigned Long vector.
     * @return a plain Long vector.
     */
    public static PlainLongVector create(LongVector longVector) {
        return new PlainLongVector(longVector);
    }

    /**
     * Create a random plain Long vector.
     *
     * @param num          num.
     * @param secureRandom the random states.
     * @return a plain Long vector.
     */
    public static PlainLongVector createRandom(int num, SecureRandom secureRandom) {
        return new PlainLongVector(LongVector.createRandom(num, secureRandom));
    }

    /**
     * Create a plain all-one Long vector.
     *
     * @param num num.
     * @return a plain Long vector.
     */
    public static PlainLongVector createOnes(int num) {
        return new PlainLongVector(LongVector.createOnes(num));
    }

    /**
     * Create a plain all-zero Long vector.
     *
     * @param num num.
     * @return a plain Long vector.
     */
    public static PlainLongVector createZeros(int num) {
        return new PlainLongVector(LongVector.createZeros(num));
    }

    /**
     * the Long vector
     */
    private LongVector longVector;

    /**
     * private constructor.
     */
    private PlainLongVector(LongVector longVector) {
        this.longVector = longVector;
    }

    @Override
    public LongVector[] getVectors() {
        return new LongVector[]{longVector};
    }

    @Override
    public void setVectors(LongVector... vec){
        assert vec.length == 1;
        longVector = vec[0];
    }

    @Override
    public PlainLongVector[] split(int[] splitNums) {
        MathPreconditions.checkEqual("this.num", "sum(splitNums)", this.getNum(), Arrays.stream(splitNums).sum());
        PlainLongVector[] res = new PlainLongVector[splitNums.length];
        for(int i = 0, pos = 0; i < splitNums.length; i++){
            res[i] = PlainLongVector.create(Arrays.copyOfRange(longVector.getElements(), pos, pos + splitNums[i]));
        }
        return res;
    }

    @Override
    public boolean isPlain() {
        return true;
    }

    @Override
    public PlainLongVector copy() {
        return PlainLongVector.create(longVector.copy());
    }

    @Override
    public int getNum() {
        return longVector.getNum();
    }

    @Override
    public PlainLongVector split(int splitNum) {
        LongVector splitVector = longVector.split(splitNum);
        return PlainLongVector.create(splitVector);
    }

    @Override
    public void reduce(int reduceNum) {
        longVector.reduce(reduceNum);
    }

    @Override
    public void merge(MpcVector other) {
        PlainLongVector that = (PlainLongVector) other;
        longVector.merge(that.getVectors()[0]);
    }
}
