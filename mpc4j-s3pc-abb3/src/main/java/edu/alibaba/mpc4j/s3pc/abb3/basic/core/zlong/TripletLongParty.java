package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.AbbCoreParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;

import java.util.stream.IntStream;

/**
 * Zl64c Party for three-party secret sharing
 *
 * @author Feng Han
 * @date 2023/12/15
 */
public interface TripletLongParty extends AbbCoreParty, MpcLongParty {
    /**
     * inits the protocol.
     */
    void init();

    /**
     * get an empty shared vector
     *
     * @param dataNum the number of data
     */
    TripletLongVector createZeros(int dataNum);

    @Override
    default TripletLongVector mul(MpcLongVector xi, MpcLongVector yi) {
        return mul(new MpcLongVector[]{xi}, new MpcLongVector[]{yi})[0];
    }

    @Override
    TripletLongVector[] mul(MpcLongVector[] xiArray, MpcLongVector[] yiArray);

    @Override
    TripletLongVector sub(MpcLongVector xi, MpcLongVector yi);

    @Override
    default TripletLongVector[] sub(MpcLongVector[] xiArray, MpcLongVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        TripletLongVector[] res = new TripletLongVector[xiArray.length];
        for (int i = 0; i < xiArray.length; i++) {
            res[i] = sub(xiArray[i], yiArray[i]);
        }
        return res;
//        IntStream intStream = getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
//        return intStream.mapToObj(i -> sub(xiArray[i], yiArray[i])).toArray(TripletZl64Vector[]::new);
    }

    @Override
    TripletLongVector add(MpcLongVector xi, MpcLongVector yi);

    @Override
    default TripletLongVector[] add(MpcLongVector[] xiArray, MpcLongVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        IntStream intStream = getParallel() ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i -> add(xiArray[i], yiArray[i])).toArray(TripletLongVector[]::new);
    }

    /**
     * compute: res[i] = xi[i] + constValue
     *
     * @param xi         data
     * @param constValue a plain const value
     */
    MpcLongVector add(MpcLongVector xi, long constValue);

    /**
     * compute: xi[i] = xi[i] + constValue
     *
     * @param xi         data
     * @param constValue a plain const value
     */
    void addi(MpcLongVector xi, long constValue);

    /**
     * verify mul result
     *
     * @throws MpcAbortException if the protocol is abort.
     */
    void verifyMul() throws MpcAbortException;

    /**
     * check the unverified multiplication calculations
     *
     * @throws MpcAbortException if the protocol is abort.
     */
    void checkUnverified() throws MpcAbortException;

    /**
     * verify the data is the share of zeros
     *
     * @param validBitLen valid bit length
     * @param data        data to be verified
     * @throws MpcAbortException if the protocol is abort.
     */
    void compareView4Zero(int validBitLen, TripletLongVector... data) throws MpcAbortException;
}
