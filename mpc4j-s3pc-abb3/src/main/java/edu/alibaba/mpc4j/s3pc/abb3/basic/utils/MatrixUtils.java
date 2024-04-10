package edu.alibaba.mpc4j.s3pc.abb3.basic.utils;

import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Utilities for matrix operation
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class MatrixUtils {
    public static TripletRpZ2Vector[] bitPartition(TripletRpZ2Vector[] input, EnvType envType, boolean parallel) {
        BitVector[] v0 = Arrays.stream(input).map(each -> each.getBitVectors()[0]).toArray(BitVector[]::new);
        BitVector[] v1 = Arrays.stream(input).map(each -> each.getBitVectors()[1]).toArray(BitVector[]::new);
        byte[][] transV0 = ZlDatabase.create(envType, parallel, v0).getBytesData();
        byte[][] transV1 = ZlDatabase.create(envType, parallel, v1).getBytesData();
        return IntStream.range(0, transV0.length).mapToObj(i ->
            TripletRpZ2Vector.create(new byte[][]{transV0[i], transV1[i]}, input.length)).toArray(TripletRpZ2Vector[]::new);
    }

    public static BitVector[] transAvIntoBv(LongVector data, EnvType envType, boolean parallel, int lastBitNum){
        byte[][] byteForm = Arrays.stream(data.getElements()).mapToObj(LongUtils::longToByteArray).toArray(byte[][]::new);
        BitVector[] tmp = ZlDatabase.create(64, byteForm).bitPartition(envType, parallel);
        if(lastBitNum == 64){
            return tmp;
        }else{
            return Arrays.copyOfRange(tmp, 64 - lastBitNum, 64);
        }
    }

    public static LongVector transBvIntoAv(BitVector[] data, EnvType envType, boolean parallel){
        MathPreconditions.checkGreaterOrEqual("64 >= data.length", 64, data.length);
        byte[][] tmp = ZlDatabase.create(envType, parallel, data).getBytesData();
        if(data.length == 64){
            return LongVector.create(Arrays.stream(tmp).mapToLong(LongUtils::byteArrayToLong).toArray());
        }else{
            Stream<byte[]> stream = parallel ? Arrays.stream(tmp).parallel() : Arrays.stream(tmp);
            return LongVector.create(stream.mapToLong(each -> BigIntegerUtils.byteArrayToNonNegBigInteger(each).longValue()).toArray());
        }
    }


    public static TripletRpZ2Vector[][] transposeDim(TripletRpZ2Vector[][] data) {
        MathPreconditions.checkNonNegative("data.length", data.length);
        return IntStream.range(0, data[0].length).mapToObj(i ->
                Arrays.stream(data).map(x -> x[i]).toArray(TripletRpZ2Vector[]::new))
            .toArray(TripletRpZ2Vector[][]::new);
    }

    public static TripletLongVector[][] transposeDim(TripletLongVector[][] data) {
        MathPreconditions.checkNonNegative("data.length", data.length);
        return IntStream.range(0, data[0].length).mapToObj(i ->
                Arrays.stream(data).map(x -> x[i]).toArray(TripletLongVector[]::new))
            .toArray(TripletLongVector[][]::new);
    }

    public static TripletRpZ2Vector[] flat(TripletRpZ2Vector[][] data) {
        MathPreconditions.checkNonNegative("data.length", data.length);
        return Arrays.stream(data).flatMap(Arrays::stream).toArray(TripletRpZ2Vector[]::new);
    }

    public static BitVector[] flat(BitVector[][] data) {
        MathPreconditions.checkNonNegative("data.length", data.length);
        return Arrays.stream(data).flatMap(Arrays::stream).toArray(BitVector[]::new);
    }

    public static TripletLongVector[] flat(TripletLongVector[][] data) {
        MathPreconditions.checkNonNegative("data.length", data.length);
        return Arrays.stream(data).flatMap(Arrays::stream).toArray(TripletLongVector[]::new);
    }

    public static TripletRpZ2Vector[][] intoMatrix(TripletRpZ2Vector[] data, int rowNum) {
        MathPreconditions.checkNonNegative("data.length", data.length);
        int columnNum = data.length / rowNum;
        MathPreconditions.checkEqual("columnNum * rowNum", "data.length", columnNum * rowNum, data.length);
        return IntStream.range(0, rowNum).mapToObj(i ->
                Arrays.copyOfRange(data, i * columnNum, (i + 1) * columnNum))
            .toArray(TripletRpZ2Vector[][]::new);
    }

    public static BitVector[][] intoMatrix(BitVector[] data, int rowNum) {
        MathPreconditions.checkNonNegative("data.length", data.length);
        int columnNum = data.length / rowNum;
        MathPreconditions.checkEqual("columnNum * rowNum", "data.length", columnNum * rowNum, data.length);
        return IntStream.range(0, rowNum).mapToObj(i ->
                Arrays.copyOfRange(data, i * columnNum, (i + 1) * columnNum))
            .toArray(BitVector[][]::new);
    }

    /**
     * 得到当前share的最低一位的share值
     */
    public static TripletRpZ2Vector shiftOneBit(TripletRpLongVector x) {
        int len = x.getNum();
        IntStream wIntStream = IntStream.range(0, len);
        boolean[][] tmp = new boolean[2][len];
        wIntStream.forEach(i -> {
            tmp[0][i] = (x.getVectors()[0].getElement(i) & 1) == 1;
            tmp[1][i] = (x.getVectors()[1].getElement(i) & 1) == 1;
        });
        return TripletRpZ2Vector.create(Arrays.stream(tmp).map(BinaryUtils::binaryToRoundByteArray).toArray(byte[][]::new), len);
    }
}
