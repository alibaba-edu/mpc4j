package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Interface for three-party replicated-sharing type conversion
 *
 * @author Feng Han
 * @date 2024/01/17
 */
public interface Aby3ConvParty extends ConvParty {
    /**
     * get the provider
     */
    TripletProvider getProvider();

    /**
     * get the Z2cParty
     */
    TripletZ2cParty getZ2cParty();

    /**
     * get the Zl64cParty
     */
    TripletLongParty getZl64cParty();

    /**
     * convert arithmetic sharing into binary sharing
     *
     * @param data   data to be converted
     * @param bitNum the valid bit length (how many bits should be appeared in the result)
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    TripletRpZ2Vector[] a2b(MpcLongVector data, int bitNum) throws MpcAbortException;

    /**
     * convert arithmetic sharing into binary sharing
     *
     * @param data   data to be converted
     * @param bitNum the valid bit length (how many bits should be appeared in the result)
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default TripletRpZ2Vector[][] a2b(MpcLongVector[] data, int bitNum) throws MpcAbortException {
        TripletRpLongVector[] type = (TripletRpLongVector[]) data;
        TripletRpLongVector all = TripletRpLongVector.mergeWithPadding(type);
        TripletRpZ2Vector[] tmp = a2b(all, bitNum);
        int[] bits = Arrays.stream(type).mapToInt(TripletRpLongVector::getNum).toArray();
        TripletRpZ2Vector[][] res = new TripletRpZ2Vector[type.length][bitNum];
        for (int i = 0; i < tmp.length; i++) {
            TripletRpZ2Vector[] split = tmp[i].splitWithPadding(bits);
            for (int j = 0; j < split.length; j++) {
                res[j][i] = split[j];
            }
        }
        return res;
    }

    /**
     * convert binary sharing into arithmetic sharing
     *
     * @param data data to be converted
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    TripletRpLongVector b2a(MpcZ2Vector[] data) throws MpcAbortException;

    /**
     * convert binary sharing into arithmetic sharing
     *
     * @param data data to be converted
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default TripletRpLongVector[] b2a(MpcZ2Vector[][] data) throws MpcAbortException {
        for (int i = 1; i < data.length; i++) {
            MathPreconditions.checkEqual("data[0].length", "data[i].length", data[0].length, data[i].length);
        }
        TripletRpZ2Vector[] merge = IntStream.range(0, data[0].length).mapToObj(i ->
                TripletRpZ2Vector.mergeWithPadding(Arrays.stream(data).map(each -> (TripletRpZ2Vector) each[i]).toArray(TripletRpZ2Vector[]::new)))
            .toArray(TripletRpZ2Vector[]::new);
        TripletRpLongVector tmpRes = b2a(merge);
        int[] dataNums = Arrays.stream(data).mapToInt(x -> x[0].bitNum()).toArray();
        return tmpRes.splitWithPadding(dataNums);
    }

    /**
     * convert one-bit binary sharing into arithmetic sharing
     *
     * @param data data to be converted
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    TripletRpLongVector bit2a(MpcZ2Vector data) throws MpcAbortException;

    /**
     * convert one-bit binary sharing into arithmetic sharing
     *
     * @param data data to be converted
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default TripletRpLongVector[] bit2a(MpcZ2Vector[] data) throws MpcAbortException {
        TripletRpZ2Vector[] tmp = Arrays.stream(data).map(ea -> (TripletRpZ2Vector) ea).toArray(TripletRpZ2Vector[]::new);
        TripletRpZ2Vector merge = TripletRpZ2Vector.mergeWithPadding(tmp);
        TripletRpLongVector transRes = bit2a(merge);
        return transRes.splitWithPadding(Arrays.stream(data).mapToInt(MpcZ2Vector::bitNum).toArray());
    }

    /**
     * compute [a]^A · [b]^B = [ab]^A
     *
     * @param a arithmetic sharing
     * @param b binary sharing
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    TripletRpLongVector aMulB(MpcLongVector a, MpcZ2Vector b) throws MpcAbortException;

    /**
     * compute [a]^A · [b]^B = [ab]^A
     *
     * @param a arithmetic sharing
     * @param b binary sharing
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default TripletRpLongVector[] aMulB(MpcLongVector[] a, MpcZ2Vector[] b) throws MpcAbortException {
        TripletRpLongVector mergeLong = TripletRpLongVector.mergeWithPadding((TripletRpLongVector[]) a);
        TripletRpZ2Vector mergeBit = TripletRpZ2Vector.mergeWithPadding((TripletRpZ2Vector[]) b);
        return aMulB(mergeLong, mergeBit).splitWithPadding(Arrays.stream(a).mapToInt(MpcVector::getNum).toArray());
    }

    /**
     * get the binary representation of a[index], index = 0 for the highest sign bit, and index = 63 for the lowest bit of long value
     *
     * @param a     arithmetic sharing
     * @param index which bit should be extracted
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    TripletRpZ2Vector bitExtraction(MpcLongVector a, int index) throws MpcAbortException;

    /**
     * get the binary representation of a[index], index = 0 for the highest sign bit, and index = 63 for the lowest bit of long value
     *
     * @param a     arithmetic sharing
     * @param index which bit should be extracted
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default TripletRpZ2Vector[] bitExtraction(MpcLongVector[] a, int index) throws MpcAbortException {
        TripletRpLongVector mergeLong = TripletRpLongVector.mergeWithPadding((TripletRpLongVector[]) a);
        return bitExtraction(mergeLong, index).splitWithPadding(Arrays.stream(a).mapToInt(MpcVector::getNum).toArray());
    }
}
