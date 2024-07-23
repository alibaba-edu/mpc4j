package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodePartyOutput;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Single single-point GF2K-VODE sender output.
 * <p>
 * The sender gets (x, t) with t = q + Δ · x, where Δ and q is owned by the receiver, and only the α-th x is non-zero.
 * Here Δ · x is done by directly treating the subfield element x as a field element.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Gf2kSspVodeSenderOutput implements PcgPartyOutput, Gf2kVodePartyOutput {
    /**
     * field
     */
    private final Dgf2k field;
    /**
     * α
     */
    private int alpha;
    /**
     * x[α]
     */
    private byte[] xAlpha;
    /**
     * t_i
     */
    private byte[][] t;

    /**
     * Creates a sender output.
     *
     * @param field  field.
     * @param alpha  α.
     * @param xAlpha x[α].
     * @param t      t_i.
     * @return a sender output.
     */
    public static Gf2kSspVodeSenderOutput create(Dgf2k field, int alpha, byte[] xAlpha, byte[][] t) {
        Gf2kSspVodeSenderOutput senderOutput = new Gf2kSspVodeSenderOutput(field);
        Gf2e subfield = field.getSubfield();
        MathPreconditions.checkPositive("num", t.length);
        MathPreconditions.checkNonNegativeInRange("α", alpha, t.length);
        senderOutput.alpha = alpha;
        assert subfield.validateNonZeroElement(xAlpha);
        senderOutput.xAlpha = xAlpha;
        senderOutput.t = Arrays.stream(t)
            .peek(ti -> {
                assert field.validateElement(ti);
            })
            .toArray(byte[][]::new);
        return senderOutput;
    }

    /**
     * Creates a random sender output.
     *
     * @param receiverOutput receiver output.
     * @param secureRandom   random state.
     * @return a random sender output.
     */
    public static Gf2kSspVodeSenderOutput create(Gf2kSspVodeReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        MathPreconditions.checkPositive("num", num);
        Dgf2k field = receiverOutput.getField();
        Gf2e subfield = receiverOutput.getSubfield();
        int alpha = secureRandom.nextInt(num);
        byte[] xAlpha = subfield.createNonZeroRandom(secureRandom);
        byte[] delta = receiverOutput.getDelta();
        byte[][] t = IntStream.range(0, num)
            .mapToObj(i -> {
                if (i != alpha) {
                    return BytesUtils.clone(receiverOutput.getQ(i));
                } else {
                    byte[] ti = field.mixMul(xAlpha, delta);
                    field.addi(ti, receiverOutput.getQ(i));
                    return ti;
                }
            })
            .toArray(byte[][]::new);
        return create(field, alpha, xAlpha, t);
    }

    /**
     * private constructor.
     *
     * @param field field.
     */
    private Gf2kSspVodeSenderOutput(Dgf2k field) {
        this.field = field;
    }

    @Override
    public Dgf2k getField() {
        return field;
    }

    /**
     * Gets α.
     *
     * @return α.
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * Gets x[α].
     *
     * @return x[α].
     */
    public byte[] getAlphaX() {
        return xAlpha;
    }

    /**
     * Gets x[index].
     *
     * @param index index.
     * @return x[index], where x is non-zero when index = α.
     */
    public byte[] getX(int index) {
        MathPreconditions.checkNonNegativeInRange("index", index, t.length);
        if (index == alpha) {
            return xAlpha;
        } else {
            return field.getSubfield().createZero();
        }
    }

    /**
     * Gets t[index].
     *
     * @param index index.
     * @return t[index].
     */
    public byte[] getT(int index) {
        return t[index];
    }

    @Override
    public int getNum() {
        return t.length;
    }
}
