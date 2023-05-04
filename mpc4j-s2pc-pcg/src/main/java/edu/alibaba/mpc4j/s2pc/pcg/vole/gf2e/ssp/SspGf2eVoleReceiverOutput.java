package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.ssp;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * Single single-point VOLE receiver output.
 * <p>
 * The receiver gets (Δ, q) with t = q + Δ · x, where x and t are owned by the sender, and there are only one non-zero x.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SspGf2eVoleReceiverOutput {
    /**
     * the GF2E instance
     */
    private Gf2e gf2e;
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
     * @param delta  Δ.
     * @param qArray q array.
     * @return a sender output.
     */
    public static SspGf2eVoleReceiverOutput create(Gf2e gf2e, byte[] delta, byte[][] qArray) {
        SspGf2eVoleReceiverOutput senderOutput = new SspGf2eVoleReceiverOutput();
        senderOutput.gf2e = gf2e;
        assert gf2e.validateRangeElement(delta) : "Δ must be in range [0, 2^" + gf2e.getL() + "): " + Hex.toHexString(delta);
        senderOutput.delta = BytesUtils.clone(delta);
        assert qArray.length > 0 : "# of r0 must be greater than 0: " + qArray.length;
        senderOutput.qs = Arrays.stream(qArray)
            .peek(q -> {
                assert gf2e.validateRangeElement(q) : "q must be in range [0, 2^" + gf2e.getL() + "): " + Hex.toHexString(q);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        return senderOutput;
    }

    /**
     * private constructor.
     */
    private SspGf2eVoleReceiverOutput() {
        // empty
    }

    /**
     * Gets the GF2E instance.
     *
     * @return the GF2E instance.
     */
    public Gf2e getGf2e() {
        return gf2e;
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
     * Gets q.
     *
     * @param index the index.
     * @return q.
     */
    public byte[] getQ(int index) {
        return qs[index];
    }

    /**
     * Gets num.
     *
     * @return num.
     */
    public int getNum() {
        return qs.length;
    }
}
