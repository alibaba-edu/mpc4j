package edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * the replicated shared zl64 vector in the protocols which verify multiplication with mac
 *
 * @author Feng Han
 * @date 2023/01/08
 */
public class TripletRpLongMacVector extends TripletRpLongVector implements TripletLongVector {
    /**
     * Create a plain Long vector with the assigned value.
     *
     * @param macIndex  the index of keys for generating the mac
     * @param values    the assigned values.
     * @param macValues the result of mac
     * @return a plain Long vector.
     */
    public static TripletRpLongMacVector create(int macIndex, long[][] values, long[][] macValues) {
        if (macIndex == 0) {
            return new TripletRpLongMacVector(Arrays.stream(values).map(LongVector::create).toArray(LongVector[]::new));
        } else {
            return new TripletRpLongMacVector(macIndex,
                Arrays.stream(values).map(LongVector::create).toArray(LongVector[]::new),
                Arrays.stream(macValues).map(LongVector::create).toArray(LongVector[]::new));
        }
    }

    /**
     * Creates a shared long vector with mac
     *
     * @param macIndex the index of keys for generating the mac
     * @param innerVec the assigned values.
     * @param macVec   the result of mac
     * @return a shared vector.
     */
    public static TripletRpLongMacVector create(int macIndex, LongVector[] innerVec, LongVector[] macVec) {
        return new TripletRpLongMacVector(macIndex, innerVec, macVec);
    }

    /**
     * Creates a shared long vector without mac
     *
     * @param values the assigned values.
     * @return a shared vector.
     */
    public static TripletRpLongMacVector create(long[][] values) {
        return create(0, values, null);
    }

    /**
     * Creates a shared long vector without mac
     *
     * @param innerVec the assigned values.
     * @return a shared vector.
     */
    public static TripletRpLongMacVector create(LongVector... innerVec) {
        return new TripletRpLongMacVector(innerVec);
    }

    /**
     * Creates a shared long mac vector from an existing TripletRpZl64Vector
     *
     * @param other    the existing values.
     * @param needCopy copy the original data or not
     * @return a shared vector.
     */
    public static TripletRpLongMacVector create(TripletRpLongVector other, boolean needCopy) {
        if(other instanceof TripletRpLongMacVector){
            TripletRpLongMacVector that = (TripletRpLongMacVector) other;
            if (needCopy) {
                if (that.getMacIndex() > 0) {
                    return create(that.getMacIndex(),
                        Arrays.stream(that.getVectors()).map(LongVector::copy).toArray(LongVector[]::new),
                        Arrays.stream(that.getMacVec()).map(LongVector::copy).toArray(LongVector[]::new));
                } else {
                    return create(Arrays.stream(that.getVectors()).map(LongVector::copy).toArray(LongVector[]::new));
                }
            } else {
                if (that.getMacIndex() > 0) {
                    return create(that.getMacIndex(), that.getVectors(), that.getMacVec());
                } else {
                    return create(that.getVectors());
                }
            }
        }else{
            return new TripletRpLongMacVector(needCopy
                ? Arrays.stream(other.innerVec).map(LongVector::copy).toArray(LongVector[]::new)
                : other.innerVec);
        }
    }

    /**
     * Creates a empty shared long vector without mac information
     *
     * @param dataNum the number of elements
     * @return a shared vector.
     */
    public static TripletRpLongMacVector createEmpty(int dataNum) {
        return new TripletRpLongMacVector(0,
            IntStream.range(0, 2).mapToObj(i -> LongVector.createZeros(dataNum)).toArray(LongVector[]::new),
            IntStream.range(0, 2).mapToObj(i -> LongVector.createZeros(dataNum)).toArray(LongVector[]::new)
        );
    }

    /**
     * the index of key for generating mac
     */
    private int macIndex;
    /**
     * the Long vector
     */
    private LongVector[] macVec;

    /**
     * private constructor.
     */
    private TripletRpLongMacVector(int macIndex, LongVector[] innerVec, LongVector[] macVec) {
        super(innerVec);
        MathPreconditions.checkNonNegative("macIndex >= 0", macIndex);
        MathPreconditions.checkEqual("macValues.length", "2", macVec.length, 2);
        MathPreconditions.checkEqual("innerVec[0].getNum()", "macVec[0].getNum()", innerVec[0].getNum(), macVec[0].getNum());
        MathPreconditions.checkEqual("macVec[0].getNum()", "macVec[1].getNum()", macVec[0].getNum(), macVec[1].getNum());
        this.macIndex = macIndex;
        this.macVec = macVec;
    }

    /**
     * private constructor. no mac
     */
    private TripletRpLongMacVector(LongVector[] innerVec) {
        super(innerVec);
        this.macIndex = 0;
        this.macVec = null;
    }

    @Override
    public TripletRpLongMacVector copyToNew(int startIndex, int endIndex) {
        if (macIndex == 0) {
            return new TripletRpLongMacVector(Arrays.stream(innerVec).map(each ->
                LongVector.copyOfRange(each, startIndex, endIndex)).toArray(LongVector[]::new));
        } else {
            return new TripletRpLongMacVector(macIndex,
                Arrays.stream(innerVec).map(each -> LongVector.copyOfRange(each, startIndex, endIndex)).toArray(LongVector[]::new),
                Arrays.stream(macVec).map(each -> LongVector.copyOfRange(each, startIndex, endIndex)).toArray(LongVector[]::new));
        }
    }

    @Override
    public TripletRpLongMacVector copyOfRange(int startIndex, int endIndex) {
        LongVector[] inner = Arrays.stream(this.innerVec).map(each ->
            LongVector.copyOfRange(each, startIndex, endIndex)).toArray(LongVector[]::new);
        if (this.macIndex > 0) {
            LongVector[] mac = Arrays.stream(this.macVec).map(each ->
                LongVector.copyOfRange(each, startIndex, endIndex)).toArray(LongVector[]::new);
            return create(this.macIndex, inner, mac);
        }else{
            return create(inner);
        }
    }

    @Override
    public TripletRpLongMacVector copy() {
        if (macIndex > 0) {
            return create(macIndex,
                Arrays.stream(innerVec).map(LongVector::copy).toArray(LongVector[]::new),
                Arrays.stream(macVec).map(LongVector::copy).toArray(LongVector[]::new));
        } else {
            return create(innerVec[0].copy(), innerVec[1].copy());
        }
    }

    @Override
    public TripletRpLongMacVector split(int splitNum) {
        if (macIndex > 0) {
            LongVector[] innerNew = Arrays.stream(innerVec).map(x -> x.split(splitNum)).toArray(LongVector[]::new);
            LongVector[] macNew = Arrays.stream(macVec).map(x -> x.split(splitNum)).toArray(LongVector[]::new);
            return create(macIndex, innerNew, macNew);
        } else {
            return (TripletRpLongMacVector) super.split(splitNum);
        }
    }

    @Override
    public TripletRpLongMacVector[] split(int[] splitNums) {
        LongVector[] r0 = innerVec[0].split(splitNums);
        LongVector[] r1 = innerVec[1].split(splitNums);
        if (macIndex > 0) {
            LongVector[] m0 = macVec[0].split(splitNums);
            LongVector[] m1 = macVec[1].split(splitNums);
            return IntStream.range(0, splitNums.length).mapToObj(i ->
                    create(macIndex, new LongVector[]{r0[i], r1[i]}, new LongVector[]{m0[i], m1[i]}))
                .toArray(TripletRpLongMacVector[]::new);
        } else {
            return IntStream.range(0, splitNums.length).mapToObj(i -> create(r0[i], r1[i])).toArray(TripletRpLongMacVector[]::new);
        }
    }

    @Override
    public void reduce(int reduceNum) {
        super.reduce(reduceNum);
        if (macIndex > 0) {
            Arrays.stream(macVec).forEach(x -> x.reduce(reduceNum));
        }
    }

    @Override
    public void merge(MpcVector other) {
        for (int i = 0; i < innerVec.length; i++) {
            innerVec[i].merge(((TripletLongVector)other).getVectors()[i]);
        }
        if(other instanceof TripletRpLongMacVector){
            TripletRpLongMacVector that = (TripletRpLongMacVector) other;
            if(this.macIndex == that.getMacIndex() && this.macIndex > 0){
                for (int i = 0; i < macVec.length; i++) {
                    macVec[i].merge(that.getVectors()[i]);
                }
            }else{
                deleteMac();
            }
        }else{
            deleteMac();
        }
    }

    @Override
    public void setElements(TripletLongVector data, int sourceStartIndex, int targetStartIndex, int copyLen) {
        if(data instanceof TripletRpLongMacVector && this.macVec != null){
            TripletRpLongMacVector that = (TripletRpLongMacVector) data;
            if(that.macIndex == 0 || (macIndex != that.macIndex && macIndex > 0)){
                // if the current vector has mac and not equal to the other data's mac, or the other data has no mac
                this.deleteMac();
            }else{
                if(macIndex == 0){
                    macIndex = that.macIndex;
                }else{
                    MathPreconditions.checkEqual("that.macIndex", "this.macIndex", that.macIndex, this.macIndex);
                }
                for (int i = 0; i < 2; i++) {
                    if (that.macIndex > 0) {
                        macVec[i].setValues(that.getMacVec()[i], sourceStartIndex, targetStartIndex, copyLen);
                    }
                }
            }
        }else{
            this.deleteMac();
        }
        for (int i = 0; i < 2; i++) {
            innerVec[i].setValues(data.getVectors()[i], sourceStartIndex, targetStartIndex, copyLen);
        }
    }

    /**
     * delete the mac values into a standard rp vector
     */
    public void deleteMac() {
        macIndex = 0;
        macVec = null;
    }

    /**
     * get the mac values
     */
    public LongVector[] getMacVec() {
        return macVec;
    }

    /**
     * set the mac values
     */
    public void setMacVec(LongVector... vec) {
        assert vec.length == 2;
        macVec = vec;
    }

    /**
     * get the index of mac key
     */
    public int getMacIndex() {
        return macIndex;
    }

    /**
     * set the index of mac key
     */
    public void setMacIndex(int maxIndex) {
        MathPreconditions.checkNonNegative("macIndex >= 0", macIndex);
        this.macIndex = maxIndex;
    }

    @Override
    public TripletRpLongMacVector shiftLeft(int sLen, int keepLen) {
        long[][] res = new long[2][keepLen];
        int copyLen = Math.min(getNum() - sLen, keepLen);
        IntStream.range(0, 2).forEach(i -> System.arraycopy(innerVec[i].getElements(), sLen, res[i], 0, copyLen));
        if (macIndex > 0) {
            long[][] macRes = new long[2][keepLen];
            IntStream.range(0, 2).forEach(i -> System.arraycopy(macVec[i].getElements(), sLen, macRes[i], 0, copyLen));
            return create(macIndex, res, macRes);
        } else {
            return create(res);
        }
    }

    @Override
    public TripletRpLongMacVector shiftRight(int sLen, int keepLen) {
        long[][] res = new long[2][keepLen];
        int copyLen = Math.min(getNum() - sLen, keepLen);
        int srcStartIndex = getNum() - sLen - copyLen;
        int destStartIndex = keepLen - copyLen;
        IntStream.range(0, 2).forEach(i -> System.arraycopy(innerVec[i].getElements(), srcStartIndex, res[i], destStartIndex, copyLen));
        if (macIndex > 0) {
            long[][] macRes = new long[2][keepLen];
            IntStream.range(0, 2).forEach(i -> System.arraycopy(macVec[i].getElements(), srcStartIndex, macRes[i], destStartIndex, copyLen));
            return create(macIndex, res, macRes);
        } else {
            return create(res);
        }
    }

    @Override
    public TripletLongVector shiftBitRight(int shiftLen) {
        MathPreconditions.checkGreaterOrEqual("64 >= shiftLen", 64, shiftLen);
        LongVector[] newVec = new LongVector[2];
        for (int dim = 0; dim < 2; dim++) {
            long[] tmpRes = Arrays.stream(innerVec[dim].getElements()).map(x -> x >> shiftLen).toArray();
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
                if(macIndex > 0){
                    long[] tmpMac = new long[beforeLen + paddingNum];
                    System.arraycopy(macVec[i].getElements(), 0, tmpMac, 0, beforeLen);
                    macVec[i] = LongVector.create(tmpMac);
                }
            }
        }
    }

    @Override
    public void setPointsWithFixedSpace(TripletLongVector source, int startPos, int num, int sepDistance) {
        for (int i = 0; i < innerVec.length; i++) {
            innerVec[i].setElementsByInterval(source.getVectors()[i], startPos, num, sepDistance);
        }
        if (source instanceof TripletRpLongMacVector) {
            TripletRpLongMacVector other = (TripletRpLongMacVector) source;
            if(other.getMacIndex() > 0 && this.macIndex > 0 && other.getMacIndex() == this.macIndex){
                for (int i = 0; i < innerVec.length; i++) {
                    macVec[i].setElementsByInterval(other.getMacVec()[i], startPos, num, sepDistance);
                }
            }else{
                this.deleteMac();
            }
        }else{
            this.deleteMac();
        }
    }

    @Override
    public TripletLongVector getPointsWithFixedSpace(int startPos, int num, int sepDistance) {
        LongVector[] tmp = Arrays.stream(innerVec).map(each -> each.getElementsByInterval(startPos, num, sepDistance)).toArray(LongVector[]::new);
        if(this.macIndex > 0){
            LongVector[] tmpMac = Arrays.stream(macVec).map(each -> each.getElementsByInterval(startPos, num, sepDistance)).toArray(LongVector[]::new);
            return create(macIndex, tmp, tmpMac);
        }else{
            return create(tmp);
        }
    }
}
