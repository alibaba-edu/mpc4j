package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Random OSN receiver output. The receiver holds π and (\vec Δ) such that (\vec Δ) = π(\vec a) ⊕ (\vec b).
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class RosnReceiverOutput implements PcgPartyOutput {
    /**
     * num
     */
    private int num;
    /**
     * π
     */
    private int[] pi;
    /**
     * element byte length
     */
    private int byteLength;
    /**
     * (\vec Δ) such that (\vec Δ) = π(\vec a) ⊕ (\vec b)
     */
    private byte[][] deltas;

    /**
     * Creates a receiver output.
     *
     * @param pi     π.
     * @param deltas \vec Δ.
     * @return a receiver output.
     */
    public static RosnReceiverOutput create(int[] pi, byte[][] deltas) {
        RosnReceiverOutput receiverOutput = new RosnReceiverOutput();
        Preconditions.checkArgument(PermutationNetworkUtils.validPermutation(pi));
        receiverOutput.num = pi.length;
        receiverOutput.pi = pi;
        MathPreconditions.checkEqual("n", "Δs.length", receiverOutput.num, deltas.length);
        receiverOutput.byteLength = deltas[0].length;
        MathPreconditions.checkPositive("byte_length", receiverOutput.byteLength);
        IntStream.range(0, receiverOutput.num).forEach(i ->
            MathPreconditions.checkEqual(
                "λ", "Δs[" + i + "].length", receiverOutput.byteLength, deltas[i].length
            )
        );
        receiverOutput.deltas = deltas;

        return receiverOutput;
    }

    /**
     * Creates a random receiver output.
     *
     * @param num          num.
     * @param secureRandom random state.
     * @return a random receiver output.
     */
    public static RosnReceiverOutput createRandom(int num, int byteLength, SecureRandom secureRandom) {
        RosnReceiverOutput receiverOutput = new RosnReceiverOutput();
        // generate random π
        MathPreconditions.checkGreater("n", num, 1);
        receiverOutput.num = num;
        receiverOutput.pi = PermutationNetworkUtils.randomPermutation(num, secureRandom);
        // generate random (\vec Δ)
        MathPreconditions.checkPositive("byte_length", byteLength);
        receiverOutput.byteLength = byteLength;
        receiverOutput.deltas = BytesUtils.randomByteArrayVector(num, byteLength, secureRandom);

        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private RosnReceiverOutput() {
        // empty
    }

    /**
     * Gets Δ[i].
     *
     * @param i i.
     * @return Δ[i].
     */
    public byte[] getDelta(int i) {
        return deltas[i];
    }

    /**
     * Gets Δs.
     *
     * @return Δs.
     */
    public byte[][] getDeltas() {
        return deltas;
    }

    /**
     * Gets permutation π.
     *
     * @return π.
     */
    public int[] getPi() {
        return pi;
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
