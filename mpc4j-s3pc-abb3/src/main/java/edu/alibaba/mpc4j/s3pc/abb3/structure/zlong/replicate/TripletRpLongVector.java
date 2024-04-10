package edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * the replicated shared zl64 vector
 *
 * @author Feng Han
 * @date 2023/01/08
 */
public class TripletRpLongVector implements TripletLongVector {
    /**
     * Create a plain Long vector with the assigned value.
     *
     * @param values the assigned values.
     * @return a plain Long vector.
     */
    public static TripletRpLongVector create(long[]... values) {
        MathPreconditions.checkEqual("values.length", "2", values.length, 2);
        return new TripletRpLongVector(Arrays.stream(values).map(LongVector::create).toArray(LongVector[]::new));
    }

    /**
     * Creates a plain Long vector with the assigned Long vector.
     *
     * @param longVectors the assigned Long vector.
     * @return a plain Long vector.
     */
    public static TripletRpLongVector create(LongVector... longVectors) {
        MathPreconditions.checkEqual("values.length", "2", longVectors.length, 2);
        return new TripletRpLongVector(longVectors);
    }

    public static TripletRpLongVector createZeros(int dataNum) {
        return new TripletRpLongVector(IntStream.range(0, 2).mapToObj(i -> LongVector.createZeros(dataNum)).toArray(LongVector[]::new));
    }

    /**
     * merge the data with padding zeros in front of each data, making its length mod 8 = 0
     *
     * @param data the assigned Long vector.
     * @return a shared Long vector.
     */
    public static TripletRpLongVector mergeWithPadding(TripletRpLongVector[] data) {
        if(data.length == 1){
            return data[0].copy();
        }
        int totalNum = Arrays.stream(data).mapToInt(x -> CommonUtils.getByteLength(x.getNum())).sum() << 3;
        TripletRpLongVector res = createZeros(totalNum);
        for (int i = 0, targetStartIndex = 0; i < data.length; i++) {
            int resIndex = (data[i].getNum() & 7);
            targetStartIndex += (resIndex == 0 ? 0 : 8 - resIndex);
            res.setElements(data[i], 0, targetStartIndex, data[i].getNum());
            targetStartIndex += data[i].getNum();
        }
        return res;
    }

    /**
     * the Long vector
     */
    LongVector[] innerVec;

    /**
     * private constructor.
     */
    TripletRpLongVector(LongVector[] innerVec) {
        MathPreconditions.checkEqual("innerVec.length", "2", innerVec.length, 2);
        MathPreconditions.checkEqual("innerVec[0].getNum()", "innerVec[1].getNum()", innerVec[0].getNum(), innerVec[1].getNum());
        this.innerVec = innerVec;
    }

    @Override
    public TripletRpLongVector copyOfRange(int startIndex, int endIndex) {
        return new TripletRpLongVector(Arrays.stream(this.innerVec).map(each ->
            LongVector.copyOfRange(each, startIndex, endIndex)).toArray(LongVector[]::new));
    }

    public TripletRpLongVector copyToNew(int startIndex, int endIndex) {
        return new TripletRpLongVector(Arrays.stream(innerVec).map(each ->
            LongVector.copyOfRange(each, startIndex, endIndex)).toArray(LongVector[]::new));
    }

    public TripletRpLongVector[] splitWithPadding(int[] splitNums) {
        if(splitNums.length == 1){
            return new TripletRpLongVector[]{this.copyToNew(getNum() - splitNums[0], getNum())};
        }
        TripletRpLongVector[] res = new TripletRpLongVector[splitNums.length];
        for (int i = 0, startIndex = 0; i < splitNums.length; i++) {
            int resNum = splitNums[i] & 7;
            startIndex += (resNum == 0 ? 0 : 8 - resNum);
            int endIndex = startIndex + splitNums[i];
            res[i] = copyToNew(startIndex, endIndex);
            startIndex = endIndex;
        }
        return res;
    }

    @Override
    public boolean isPlain() {
        return false;
    }

    @Override
    public TripletRpLongVector copy() {
        return TripletRpLongVector.create(Arrays.stream(innerVec).map(LongVector::copy).toArray(LongVector[]::new));
    }

    @Override
    public int getNum() {
        return innerVec[0].getNum();
    }

    @Override
    public TripletRpLongVector split(int splitNum) {
        return TripletRpLongVector.create(Arrays.stream(innerVec).map(x -> x.split(splitNum)).toArray(LongVector[]::new));
    }

    @Override
    public void reduce(int reduceNum) {
        Arrays.stream(innerVec).forEach(x -> x.reduce(reduceNum));
    }

    @Override
    public void merge(MpcVector other) {
        TripletRpLongVector that = (TripletRpLongVector) other;
        innerVec[0].merge(that.getVectors()[0]);
        innerVec[1].merge(that.getVectors()[1]);
    }

    @Override
    public LongVector[] getVectors() {
        return innerVec;
    }

    @Override
    public void setVectors(LongVector... vec) {
        assert vec.length == 2;
        innerVec = vec;
    }

    @Override
    public TripletRpLongVector[] split(int[] splitNums) {
        LongVector[] r0 = innerVec[0].split(splitNums);
        LongVector[] r1 = innerVec[1].split(splitNums);
        return IntStream.range(0, splitNums.length).mapToObj(i -> create(r0[i], r1[i])).toArray(TripletRpLongVector[]::new);
    }


    @Override
    public void setElements(TripletLongVector data, int sourceStartIndex, int targetStartIndex, int copyLen) {
        TripletRpLongVector that = (TripletRpLongVector) data;
        innerVec[0].setValues(that.getVectors()[0], sourceStartIndex, targetStartIndex, copyLen);
        innerVec[1].setValues(that.getVectors()[1], sourceStartIndex, targetStartIndex, copyLen);
    }

    @Override
    public TripletRpLongVector shiftLeft(int sLen, int keepLen) {
        long[][] res = new long[2][keepLen];
        int copyLen = Math.min(getNum() - sLen, keepLen);
        IntStream.range(0, 2).forEach(i -> System.arraycopy(innerVec[i].getElements(), sLen, res[i], 0, copyLen));
        return create(res);
    }

    @Override
    public TripletRpLongVector shiftRight(int sLen, int keepLen) {
        long[][] res = new long[2][keepLen];
        int copyLen = Math.min(getNum() - sLen, keepLen);
        int srcStartIndex = getNum() - sLen - copyLen;
        int destStartIndex = keepLen - copyLen;
        IntStream.range(0, 2).forEach(i -> System.arraycopy(innerVec[i].getElements(), srcStartIndex, res[i], destStartIndex, copyLen));
        return create(res);
    }

    @Override
    public TripletLongVector shiftBitRight(int shiftLen){
        MathPreconditions.checkGreaterOrEqual("64 >= shiftLen", 64, shiftLen);
        LongVector[] newVec = new LongVector[2];
        for(int dim = 0; dim < 2; dim++){
            long[] tmpRes = Arrays.stream(innerVec[dim].getElements()).map(x -> x>>shiftLen).toArray();
            newVec[dim] = LongVector.create(tmpRes);
        }
        return create(newVec);
    }

    @Override
    public void paddingZeros(int paddingNum){
        MathPreconditions.checkNonNegative("paddingNum", paddingNum);
        int beforeLen = getNum();
        if(paddingNum > 0){
            for(int i = 0; i < 2; i++){
                long[] tmp = new long[beforeLen + paddingNum];
                System.arraycopy(innerVec[i].getElements(), 0, tmp, 0, beforeLen);
                innerVec[i] = LongVector.create(tmp);
            }
        }
    }

    @Override
    public void setPointsWithFixedSpace(TripletLongVector source, int startPos, int num, int sepDistance) {
        for(int i = 0; i < innerVec.length; i++){
            innerVec[i].setElementsByInterval(source.getVectors()[i], startPos, num, sepDistance);
        }
    }

    @Override
    public TripletLongVector getPointsWithFixedSpace(int startPos, int num, int sepDistance) {
        LongVector[] tmp = Arrays.stream(innerVec).map(each -> each.getElementsByInterval(startPos, num, sepDistance)).toArray(LongVector[]::new);
        return create(tmp);
    }
}
