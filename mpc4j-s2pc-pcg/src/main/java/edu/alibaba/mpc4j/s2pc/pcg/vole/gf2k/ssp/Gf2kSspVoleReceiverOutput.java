package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * Single single-point GF2K VOLE receiver output.
 * <p>
 * The receiver gets (Δ, q) with t = q + Δ · x, where x and t are owned by the sender, and there are only one non-zero x.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Gf2kSspVoleReceiverOutput {
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
    public static Gf2kSspVoleReceiverOutput create(byte[] delta, byte[][] qArray) {
        Gf2kSspVoleReceiverOutput senderOutput = new Gf2kSspVoleReceiverOutput();
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "Δ must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(delta);
        senderOutput.delta = BytesUtils.clone(delta);
        assert qArray.length > 0 : "# of r0 must be greater than 0: " + qArray.length;
        senderOutput.qs = Arrays.stream(qArray)
            .peek(q -> {
                assert q.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "q must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(q);
            })
            .toArray(byte[][]::new);
        return senderOutput;
    }

    /**
     * private constructor.
     */
    private Gf2kSspVoleReceiverOutput() {
        // empty
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
