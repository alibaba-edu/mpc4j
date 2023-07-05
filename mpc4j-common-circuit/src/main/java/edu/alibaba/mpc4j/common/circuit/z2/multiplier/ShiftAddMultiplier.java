package edu.alibaba.mpc4j.common.circuit.z2.multiplier;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.adder.Adder;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Shift/Add Multiplier.
 *
 * @author Li Peng
 * @date 2023/6/6
 */
public class ShiftAddMultiplier extends AbstractMultiplier {
    /**
     * Adder.
     */
    Adder adder;

    public ShiftAddMultiplier(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
        this.adder = circuit.getAdder();
    }

    @Override
    public MpcZ2Vector[] mul(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        this.l = xiArray.length;
        this.num = xiArray[0].getNum();
        return Arrays.copyOfRange(mulInternal(xiArray, yiArray), l, 2 * l);
    }

    /**
     * full multiplier without truncation.
     *
     * @param xs x array in big-endian order.
     * @param ys y array in big-endian order.
     * @return result.
     */
    private MpcZ2Vector[] mulInternal(MpcZ2Vector[] xs, MpcZ2Vector[] ys) throws MpcAbortException {
        MpcZ2Vector[] result = IntStream.range(0, 2 * l).mapToObj(i -> party.createZeros(num)).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] zeros = IntStream.range(0, l).mapToObj(i -> party.createZeros(num)).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] toAdd = mux(zeros, xs, ys[ys.length - 1]);
        System.arraycopy(toAdd, 0, result, result.length - toAdd.length, toAdd.length);
        for (int i = 1; i < ys.length; i++) {
            toAdd = Arrays.copyOfRange(result, result.length - xs.length - i, result.length - i);
            toAdd = adder.add(toAdd, mux(zeros, xs, ys[ys.length - 1 - i]), false);
            System.arraycopy(toAdd, 1, result, result.length - toAdd.length - i + 1, toAdd.length - 1);
        }
        return result;
    }
}
