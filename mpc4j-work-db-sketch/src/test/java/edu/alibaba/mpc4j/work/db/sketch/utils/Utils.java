package edu.alibaba.mpc4j.work.db.sketch.utils;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2cParty;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;

import java.util.Arrays;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.EnvType.STANDARD_JDK;

public class Utils {
    static public PlainZ2Vector[] matrixTranspose(PlainZ2cParty party, MpcZ2Vector[] data) {
        int vecNumOfEachShare = data[0].getBitVectors().length;
        BitVector[][] rows = IntStream.range(0, vecNumOfEachShare).mapToObj(i ->
                        Arrays.stream(data).map(x -> x.getBitVectors()[i]).toArray(BitVector[]::new))
                .toArray(BitVector[][]::new);

        BitVector[][] transRes = Arrays.stream(rows).map(array -> {
            byte[][] tmp = ZlDatabase.create(STANDARD_JDK, false, array).getBytesData();
            return Arrays.stream(tmp).map(each -> BitVectorFactory.create(data.length, each)).toArray(BitVector[]::new);
        }).toArray(BitVector[][]::new);
        return IntStream.range(0, transRes[0].length).mapToObj(i ->
                        (PlainZ2Vector)party.create(data[0].isPlain(), Arrays.stream(transRes).map(x -> x[i]).toArray(BitVector[]::new)))
                .toArray(PlainZ2Vector[]::new);
    }
}
