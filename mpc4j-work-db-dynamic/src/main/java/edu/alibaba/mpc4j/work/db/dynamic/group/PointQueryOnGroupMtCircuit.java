package edu.alibaba.mpc4j.work.db.dynamic.group;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;

import java.util.Arrays;

/**
 * point query on group mt
 */
public class PointQueryOnGroupMtCircuit {
    /**
     * z2c party
     */
    protected final MpcZ2cParty z2cParty;
    /**
     * z2 circuit
     */
    protected final Z2IntegerCircuit circuit;
    /**
     * dimension of the database
     */
    protected int dim;

    /**
     * constructor with z2 circuit
     */
    public PointQueryOnGroupMtCircuit(Z2IntegerCircuit circuit) {
        this.z2cParty = circuit.getParty();
        this.circuit = circuit;
    }

    /**
     * constructor with z2 party and config
     */
    public PointQueryOnGroupMtCircuit(Z2CircuitConfig config, MpcZ2cParty z2cParty) {
        this.z2cParty = z2cParty;
        this.circuit = new Z2IntegerCircuit(z2cParty, config);
    }

    /**
     * point query to get the [payload, valid indicator] value of the corresponding group
     *
     * @param groupByMt group mt
     * @param key       required group key
     * @return [payload data_0, ... payload data_dim, valid indicator]
     */
    public MpcZ2Vector[] pointQuery(GroupByMt groupByMt, MpcZ2Vector[] key) throws MpcAbortException {
        int[] keyIndexes = groupByMt.getGroupKeyIndexes();
        MathPreconditions.checkEqual("key.length", "keyIndexes.length", key.length, keyIndexes.length);
        MpcZ2Vector[] extendedKey = extendInput(key, groupByMt.getData()[0].bitNum(), z2cParty);
        MpcZ2Vector eqFlag = circuit.eq(
            extendedKey,
            Arrays.stream(keyIndexes).mapToObj(i -> groupByMt.getData()[i]).toArray(MpcZ2Vector[]::new)
        );
        eqFlag = z2cParty.and(eqFlag, groupByMt.getData()[groupByMt.getValidityIndex()]);
        MpcZ2Vector[] muxRes = z2cParty.and(
            eqFlag,
            Arrays.stream(groupByMt.getValueIndexes()).mapToObj(i -> groupByMt.getData()[i]).toArray(MpcZ2Vector[]::new)
        );
        MpcZ2Vector[] xorRes = Arrays.stream(muxRes)
            .map(z2cParty::xorSelfAllElement)
            .toArray(MpcZ2Vector[]::new);
        eqFlag = z2cParty.xorSelfAllElement(eqFlag);
        MpcZ2Vector[] result = new MpcZ2Vector[xorRes.length + 1];
        System.arraycopy(xorRes, 0, result, 0, xorRes.length);
        result[result.length - 1] = eqFlag;
        return result;
    }

    /**
     * extend input data into the required length
     *
     * @param input the input
     * @param num   the number of required rows
     * @return the extended input data
     */
    public static MpcZ2Vector[] extendInput(MpcZ2Vector[] input, int num, MpcZ2cParty z2cParty) {
        Preconditions.checkArgument(input.length > 0);
        MathPreconditions.checkGreater("num > 0", num, 0);
        MathPreconditions.checkGreater("input.length > 0", input.length, 0);
        for (MpcZ2Vector vec : input) {
            MathPreconditions.checkEqual("vec.bitNum()", "1", vec.bitNum(), 1);
        }
        BitVector[][] shareData = Arrays.stream(input)
            .map(MpcZ2Vector::getBitVectors)
            .map(ea -> Arrays.stream(ea)
                .map(one -> one.get(0) ? BitVectorFactory.createOnes(num) : BitVectorFactory.createZeros(num))
                .toArray(BitVector[]::new)
            )
            .toArray(BitVector[][]::new);
        return Arrays.stream(shareData).map(ea -> z2cParty.create(input[0].isPlain(), ea)).toArray(MpcZ2Vector[]::new);
    }
}
