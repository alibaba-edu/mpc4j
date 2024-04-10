package edu.alibaba.mpc4j.s3pc.abb3.structure.zlong;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;

/**
 * Basic data structure for three-party secret sharing
 *
 * @author Feng Han
 * @date 2023/12/15
 */
public interface TripletLongVector extends MpcLongVector {

    /**
     * copy the given range of the current vector
     *
     * @param startIndex             start index of copy
     * @param endIndex the end index of the copied data
     */
    TripletLongVector copyOfRange(int startIndex, int endIndex);

    /**
     * set values: copy (data[sourceStartIndex] - data[sourceStartIndex + copyLen]) into (this[targetStartIndex], this[targetStartIndex + copyLen])
     *
     * @param data             the source vector
     * @param sourceStartIndex the start index of the copied data
     * @param targetStartIndex the start index of the target vector
     * @param copyLen          the length of the copied data
     */
    void setElements(TripletLongVector data, int sourceStartIndex, int targetStartIndex, int copyLen);

    /**
     * first get [sLen, num], then keep the first 'keepLen' elements
     *
     * @param sLen    shift length
     * @param keepLen expected length
     */
    TripletLongVector shiftLeft(int sLen, int keepLen);

    /**
     * first get [0, num - sLen], then keep the last 'keepLen' elements
     *
     * @param sLen    shift length
     * @param keepLen expected length
     */
    TripletLongVector shiftRight(int sLen, int keepLen);

    /**
     * for each element, bit shift right X bits
     *
     * @param shiftLen How many bits to shift to the right?
     */
    TripletLongVector shiftBitRight(int shiftLen);

    /**
     * padding dummy zeros after the current vectors
     *
     * @param paddingNum How many zeros should be padded
     */
    void paddingZeros(int paddingNum);


    /**
     * 基于一定间隔，设置部分位置的数据
     * @param source 从哪里取数据设置
     * @param startPos 从哪一个位置开始取
     * @param num 取多少个bit
     * @param sepDistance 取位的间隔是多少个bit
     */
    void setPointsWithFixedSpace(TripletLongVector source, int startPos, int num, int sepDistance);
    /**
     * 基于一定间隔，得到部分位置的数据
     * @param startPos 从哪一个位置开始取
     * @param num 取多少个
     * @param sepDistance 取位的间隔是多少个
     */
    TripletLongVector getPointsWithFixedSpace(int startPos, int num, int sepDistance);
}
