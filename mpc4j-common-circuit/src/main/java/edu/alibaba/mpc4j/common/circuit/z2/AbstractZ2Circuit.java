package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.stream.IntStream;

/**
 * Abstract z2 circuit.
 *
 * @author Li Peng
 * @date 2023/6/7
 */
public class AbstractZ2Circuit {
    /**
     * MPC boolean circuit party.
     */
    protected MpcZ2cParty party;

    public AbstractZ2Circuit(MpcZ2cParty party) {
        this.party = party;
    }

    public MpcZ2cParty getParty() {
        return party;
    }

    /**
     * Vector MUX operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @param ci      ci.
     * @return zi array, such that for each j, z[i] = (c ? y[i] : x[i])
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected MpcZ2Vector[] mux(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, MpcZ2Vector ci) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        MpcZ2Vector[] ciArray = IntStream.range(0, xiArray.length).mapToObj(i -> ci).toArray(MpcZ2Vector[]::new);
        return party.xor(party.and(party.xor(xiArray, yiArray), ciArray), xiArray);
    }

    protected void checkInputs(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        int l = xiArray.length;
        MathPreconditions.checkPositive("l", l);
        // check equal l.
        MathPreconditions.checkEqual("l", "y.length", l, yiArray.length);
        // check equal num for all vectors.
        int num = xiArray[0].getNum();
        IntStream.range(0, l).forEach(i -> {
            MathPreconditions.checkEqual("num", "xi.num", num, xiArray[i].getNum());
            MathPreconditions.checkEqual("num", "yi.num", num, yiArray[i].getNum());
        });
    }

    protected void checkInputs(MpcZ2Vector[] xs) {
        int l = xs.length;
        MathPreconditions.checkPositive("l", l);
        // check equal num for all vectors.
        int num = xs[0].getNum();
        IntStream.range(0, l).forEach(i -> MathPreconditions.checkEqual("num", "xi.num", num, xs[i].getNum()));
    }
}
