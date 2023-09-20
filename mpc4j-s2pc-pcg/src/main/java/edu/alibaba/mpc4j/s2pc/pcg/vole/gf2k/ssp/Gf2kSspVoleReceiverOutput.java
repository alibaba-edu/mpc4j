package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.Arrays;

/**
 * Single single-point GF2K-VOLE receiver output.
 * <p>
 * The receiver gets (Δ, q) with t = q + Δ · x, where x and t are owned by the sender, and there are only one non-zero x.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Gf2kSspVoleReceiverOutput implements PcgPartyOutput {
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
        Gf2kSspVoleReceiverOutput receiverOutput = new Gf2kSspVoleReceiverOutput();
        MathPreconditions.checkEqual("delta.length", "λ in bytes", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        receiverOutput.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("qArray.length", qArray.length);
        receiverOutput.qs = Arrays.stream(qArray)
            .peek(q ->
                MathPreconditions.checkEqual("q.length", "λ in bytes", delta.length, CommonConstants.BLOCK_BYTE_LENGTH)
            )
            .toArray(byte[][]::new);
        return receiverOutput;
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
     * Gets the assigned q.
     *
     * @param index index.
     * @return the assigned q.
     */
    public byte[] getQ(int index) {
        return qs[index];
    }

    @Override
    public int getNum() {
        return qs.length;
    }
}
