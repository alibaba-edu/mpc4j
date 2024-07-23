package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVolePartyOutput;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;

/**
 * multi single-point GF2K-VOLE sender output.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Gf2kMspVoleSenderOutput implements PcgPartyOutput, Gf2kVolePartyOutput {
    /**
     * field
     */
    private final Sgf2k field;
    /**
     * α array
     */
    private int[] alphaArray;
    /**
     * x[α]s
     */
    private byte[][] alphaXs;
    /**
     * i -> α
     */
    private TIntIntMap alphaIndexMap;
    /**
     * t array
     */
    private byte[][] ts;

    /**
     * Creates a sender output.
     *
     * @param field      field.
     * @param alphaArray α array.
     * @param alphaXs    x[α]s.
     * @param ts         t array.
     * @return a sender output.
     */
    public static Gf2kMspVoleSenderOutput create(Sgf2k field, int[] alphaArray, byte[][] alphaXs, byte[][] ts) {
        Gf2kMspVoleSenderOutput senderOutput = new Gf2kMspVoleSenderOutput(field);
        MathPreconditions.checkPositive("ts.length", ts.length);
        Gf2e subfield = field.getSubfield();
        int num = ts.length;
        MathPreconditions.checkPositiveInRangeClosed("alphaArray.length", alphaArray.length, num);
        senderOutput.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("α", alpha, num))
            .distinct()
            .sorted()
            .toArray();
        MathPreconditions.checkEqual(
            "(distinct) alphaArray.length", "alphaArray.length",
            senderOutput.alphaArray.length, alphaArray.length
        );
        MathPreconditions.checkEqual("x[α]s.length", "alphaArray.length", alphaXs.length, alphaArray.length);
        senderOutput.alphaXs = Arrays.stream(alphaXs)
            .peek(x -> Preconditions.checkArgument(subfield.validateElement(x)))
            .toArray(byte[][]::new);
        senderOutput.alphaIndexMap = new TIntIntHashMap(alphaArray.length);
        for (int alphaIndex = 0; alphaIndex < alphaArray.length; alphaIndex++) {
            senderOutput.alphaIndexMap.put(alphaArray[alphaIndex], alphaIndex);
        }
        senderOutput.ts = Arrays.stream(ts)
            .peek(t -> Preconditions.checkArgument(field.validateElement(t)))
            .toArray(byte[][]::new);
        return senderOutput;
    }

    /**
     * private constructor.
     *
     * @param field field.
     */
    private Gf2kMspVoleSenderOutput(Sgf2k field) {
        this.field = field;
    }

    @Override
    public Sgf2k getField() {
        return field;
    }

    /**
     * Gets α array.
     *
     * @return α array.
     */
    public int[] getAlphaArray() {
        return alphaArray;
    }

    /**
     * Gets the assigned x.
     *
     * @return x.
     */
    public byte[] getX(int index) {
        MathPreconditions.checkNonNegativeInRange("index", index, ts.length);
        if (alphaIndexMap.containsKey(index)) {
            return alphaXs[alphaIndexMap.get(index)];
        } else {
            return field.getSubfield().createZero();
        }
    }

    /**
     * Gets the assigned t.
     *
     * @param index index.
     * @return t.
     */
    public byte[] getT(int index) {
        return ts[index];
    }

    /**
     * Gets t array.
     *
     * @return t array.
     */
    public byte[][] getTs() {
        return ts;
    }

    @Override
    public int getNum() {
        return ts.length;
    }
}
