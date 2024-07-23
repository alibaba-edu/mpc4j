package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.stream.IntStream;

/**
 * Single Share Translation sender output.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class SstSenderOutput implements PcgPartyOutput {
    /**
     * num
     */
    private final int num;
    /**
     * permutation π
     */
    private final int[] pi;
    /**
     * element byte length
     */
    private final int byteLength;
    /**
     * Δs such that Δs = π(as) ⊕ bs
     */
    private final byte[][] deltas;

    public SstSenderOutput(int[] pi, byte[][] deltas) {
        Preconditions.checkArgument(PermutationNetworkUtils.validPermutation(pi));
        num = pi.length;
        this.pi = pi;
        MathPreconditions.checkEqual("n", "Δs.length", num, deltas.length);
        byteLength = deltas[0].length;
        MathPreconditions.checkPositive("byte_length", byteLength);
        IntStream.range(0, num).forEach(i ->
            MathPreconditions.checkEqual(
                "λ", "Δs[" + i + "].length", byteLength, deltas[i].length
            )
        );
        this.deltas = deltas;
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
