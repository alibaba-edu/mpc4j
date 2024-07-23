package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVolePartyOutput;

import java.util.Arrays;

/**
 * multi single-point GF2K-VOLE receiver output.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Gf2kMspVoleReceiverOutput implements PcgPartyOutput, Gf2kVolePartyOutput {
    /**
     * field
     */
    private final Sgf2k field;
    /**
     * Δ
     */
    private byte[] delta;
    /**
     * q array.
     */
    private byte[][] qs;

    /**
     * Creates a sender output.
     *
     * @param field  field.
     * @param delta  Δ.
     * @param qArray q array.
     * @return a sender output.
     */
    public static Gf2kMspVoleReceiverOutput create(Sgf2k field, byte[] delta, byte[][] qArray) {
        Gf2kMspVoleReceiverOutput receiverOutput = new Gf2kMspVoleReceiverOutput(field);
        Preconditions.checkArgument(field.validateElement(delta));
        receiverOutput.delta = delta;
        MathPreconditions.checkPositive("qArray.length", qArray.length);
        receiverOutput.qs = Arrays.stream(qArray)
            .peek(q -> Preconditions.checkArgument(field.validateElement(q)))
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     *
     * @param field field.
     */
    private Gf2kMspVoleReceiverOutput(Sgf2k field) {
        this.field = field;
    }

    @Override
    public Sgf2k getField() {
        return field;
    }

    /**
     * Gets Δ.
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
    }

    /**
     * Gets the assigned q.
     *
     * @param index index.
     * @return the assigned q.
     */
    public byte[] getQ(int index) {
        return qs[index];
    }

    /**
     * Gets q array.
     *
     * @return q array.
     */
    public byte[][] getQs() {
        return qs;
    }

    @Override
    public int getNum() {
        return qs.length;
    }
}
