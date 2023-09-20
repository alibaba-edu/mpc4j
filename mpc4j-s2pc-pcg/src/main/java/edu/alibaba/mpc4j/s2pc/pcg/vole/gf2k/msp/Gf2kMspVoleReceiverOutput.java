package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.Arrays;

/**
 * multi single-point GF2K-VOLE receiver output.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Gf2kMspVoleReceiverOutput implements PcgPartyOutput {
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
    public static Gf2kMspVoleReceiverOutput create(byte[] delta, byte[][] qArray) {
        Gf2kMspVoleReceiverOutput receiverOutput = new Gf2kMspVoleReceiverOutput();
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
    private Gf2kMspVoleReceiverOutput() {
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
