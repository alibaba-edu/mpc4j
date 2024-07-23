package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Random OSN sender output. The sender holds (\vec a) and (\vec b) such that (\vec Δ) = π(\vec a) ⊕ (\vec b).
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class RosnSenderOutput implements PcgPartyOutput {
    /**
     * num
     */
    private int num;
    /**
     * element byte length
     */
    private int byteLength;
    /**
     * \vec a
     */
    private byte[][] as;
    /**
     * \vec b
     */
    private byte[][] bs;

    /**
     * Creates a sender output.
     *
     * @param as \vec a.
     * @param bs \vec b.
     * @return a sender output.
     */
    public static RosnSenderOutput create(byte[][] as, byte[][] bs) {
        RosnSenderOutput senderOutput = new RosnSenderOutput();
        senderOutput.num = as.length;
        MathPreconditions.checkGreater("n", senderOutput.num, 1);
        MathPreconditions.checkEqual("n", "bs.length", senderOutput.num, bs.length);
        senderOutput.byteLength = as[0].length;
        MathPreconditions.checkPositive("byte_length", senderOutput.byteLength);
        IntStream.range(0, senderOutput.num).forEach(i -> {
            MathPreconditions.checkEqual("byte_length", "as[" + i + "].length", senderOutput.byteLength, as[i].length);
            MathPreconditions.checkEqual("byte_length", "bs[" + i + "].length", senderOutput.byteLength, bs[i].length);
        });
        senderOutput.as = as;
        senderOutput.bs = bs;
        return senderOutput;
    }

    /**
     * Creates a random sender output.
     *
     * @param receiverOutput receiver output.
     * @param secureRandom   random state.
     * @return a random receiver output.
     */
    public static RosnSenderOutput createRandom(RosnReceiverOutput receiverOutput, SecureRandom secureRandom) {
        RosnSenderOutput senderOutput = new RosnSenderOutput();
        senderOutput.num = receiverOutput.getNum();
        senderOutput.byteLength = receiverOutput.getByteLength();
        senderOutput.as = BytesUtils.randomByteArrayVector(senderOutput.num, senderOutput.byteLength, secureRandom);
        byte[][] pas = PermutationNetworkUtils.permutation(receiverOutput.getPi(), senderOutput.as);
        senderOutput.bs = IntStream.range(0, senderOutput.num)
            .mapToObj(i -> BytesUtils.xor(pas[i], receiverOutput.getDelta(i)))
            .toArray(byte[][]::new);

        return senderOutput;
    }

    /**
     * private constructor.
     */
    private RosnSenderOutput() {
        // empty
    }

    /**
     * Gets as[i].
     *
     * @param i i.
     * @return as[i].
     */
    public byte[] getA(int i) {
        return as[i];
    }

    /**
     * Gets as.
     *
     * @return as.
     */
    public byte[][] getAs() {
        return as;
    }

    /**
     * Gets bs[i].
     *
     * @param i i.
     * @return bs[i].
     */
    public byte[] getB(int i) {
        return bs[i];
    }

    /**
     * Gets bs.
     *
     * @return bs.
     */
    public byte[][] getBs() {
        return bs;
    }

    /**
     * Gets element byte length.
     *
     * @return element byte length.
     */
    public int getByteLength() {
        return byteLength;
    }

    @Override
    public int getNum() {
        return num;
    }
}
