package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.z2.adder.Adder;
import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory;
import edu.alibaba.mpc4j.common.circuit.z2.multiplier.Multiplier;
import edu.alibaba.mpc4j.common.circuit.z2.multiplier.MultiplierFactory;
import edu.alibaba.mpc4j.common.circuit.z2.psorter.Psorter;
import edu.alibaba.mpc4j.common.circuit.z2.psorter.PsorterFactory;
import edu.alibaba.mpc4j.common.circuit.z2.sorter.Sorter;
import edu.alibaba.mpc4j.common.circuit.z2.sorter.SorterFactory;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Z2 Integer Circuit.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public class Z2IntegerCircuit extends AbstractZ2Circuit {
    /**
     * adder.
     */
    private final Adder adder;
    /**
     * multiplier.
     */
    private final Multiplier multiplier;
    /**
     * sorter.
     */
    private final Sorter sorter;
    /**
     * psorter.
     */
    private final Psorter pSorter;

    public Z2IntegerCircuit(MpcZ2cParty party) {
        this(party, new Z2CircuitConfig.Builder().build());
    }

    public Z2IntegerCircuit(MpcZ2cParty party, Z2CircuitConfig config) {
        super(party);
        this.party = party;
        this.adder = AdderFactory.createAdder(config.getAdderType(), this);
        this.multiplier = MultiplierFactory.createMultiplier(config.getMultiplierType(), this);
        this.sorter = SorterFactory.createSorter(config.getSorterType(), this);
        this.pSorter = PsorterFactory.createPsorter(config.getPsorterType(), this);
    }

    /**
     * x + y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = x + y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        return add(xiArray, yiArray, false);
    }

    private MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, boolean cin) throws MpcAbortException {
        MpcZ2Vector[] zs = adder.add(xiArray, yiArray, cin);
        // ignore the highest carry_out bit.
        return Arrays.copyOfRange(zs, 1, xiArray.length + 1);
    }

    /**
     * x - y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = x - y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector[] sub(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        // x - y = x + (complement y) + 1
        return add(xiArray, party.not(yiArray), true);
    }

    /**
     * x++.
     *
     * @param xiArray xi array.
     * @return zi array, where x + 1.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector[] increaseOne(MpcZ2Vector[] xiArray) throws MpcAbortException {
        checkInputs(xiArray);
        int l = xiArray.length;
        int bitNum = xiArray[0].getNum();
        MpcZ2Vector[] ys = IntStream.range(0, l).mapToObj(i -> party.createZeros(bitNum)).toArray(MpcZ2Vector[]::new);
        return add(xiArray, ys, true);
    }

    /**
     * x * y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = x + y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector[] mul(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        return multiplier.mul(xiArray, yiArray);
    }

    /**
     * x == y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = (x == y).
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector eq(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        int l = xiArray.length;
        // bit-wise XOR and NOT
        MpcZ2Vector[] eqiArray = party.xor(xiArray, yiArray);
        eqiArray = party.not(eqiArray);
        // tree-based AND
        int logL = LongUtils.ceilLog2(l);
        for (int h = 1; h <= logL; h++) {
            int nodeNum = eqiArray.length / 2;
            MpcZ2Vector[] eqXiArray = new MpcZ2Vector[nodeNum];
            MpcZ2Vector[] eqYiArray = new MpcZ2Vector[nodeNum];
            for (int i = 0; i < nodeNum; i++) {
                eqXiArray[i] = eqiArray[i * 2];
                eqYiArray[i] = eqiArray[i * 2 + 1];
            }
            MpcZ2Vector[] eqZiArray = party.and(eqXiArray, eqYiArray);
            if (eqiArray.length % 2 == 1) {
                eqZiArray = Arrays.copyOf(eqZiArray, nodeNum + 1);
                eqZiArray[nodeNum] = eqiArray[eqiArray.length - 1];
            }
            eqiArray = eqZiArray;
        }
        return eqiArray[0];
    }

    /**
     * x ≤ y. compare for data without sign bit, which means the values of data in [0, 2^l - 1];
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = (x ≤ y).
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector leq(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        MpcZ2Vector[] result = sub(yiArray, xiArray);
        return mux(new MpcZ2Vector[]{party.not(result[0])} , new MpcZ2Vector[]{yiArray[0]}, party.xor(xiArray[0], yiArray[0]))[0];
    }

    public void sort(MpcZ2Vector[][] xiArray) throws MpcAbortException {
        Arrays.stream(xiArray).forEach(this::checkInputs);
        sorter.sort(xiArray);
    }

    public MpcZ2Vector[] psort(MpcZ2Vector[][] xiArrays, MpcZ2Vector[][] payloadArrays, PlainZ2Vector dir, boolean needPermutation, boolean needStable) throws MpcAbortException {
        Arrays.stream(xiArrays).forEach(this::checkInputs);
        if(payloadArrays != null){
            Arrays.stream(payloadArrays).forEach(this::checkInputs);
        }
        return pSorter.sort(xiArrays, payloadArrays, dir, needPermutation, needStable);
    }

    public Adder getAdder() {
        return adder;
    }
}
