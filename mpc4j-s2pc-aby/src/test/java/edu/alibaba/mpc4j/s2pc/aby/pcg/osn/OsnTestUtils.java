package edu.alibaba.mpc4j.s2pc.aby.pcg.osn;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import org.junit.Assert;

import java.util.stream.IntStream;

/**
 * OSN test utils.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class OsnTestUtils {
    /**
     * private constructor.
     */
    private OsnTestUtils() {
        // empty
    }

    /**
     * Asserts whether the Decision OSN outputs are correct.
     *
     * @param inputVector    input vector.
     * @param pi             permutation π.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(byte[][] inputVector, int[] pi,
                                    DosnPartyOutput senderOutput, DosnPartyOutput receiverOutput) {
        int num = inputVector.length;
        Assert.assertEquals(pi.length, num);
        Assert.assertEquals(senderOutput.getN(), num);
        Assert.assertEquals(receiverOutput.getN(), num);
        Assert.assertEquals(senderOutput.getByteLength(), receiverOutput.getByteLength());
        byte[][] expectOutputs = PermutationNetworkUtils.permutation(pi, inputVector);
        byte[][] actualOutputs = IntStream.range(0, num)
            .mapToObj(index -> BytesUtils.xor(senderOutput.getShare(index), receiverOutput.getShare(index)))
            .toArray(byte[][]::new);
        IntStream.range(0, num).forEach(i -> Assert.assertArrayEquals(expectOutputs[i], actualOutputs[i]));
    }

    /**
     * Asserts whether the Random OSN outputs are correct.
     *
     * @param pi             permutation π.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(int[] pi, RosnSenderOutput senderOutput, RosnReceiverOutput receiverOutput) {
        int num = pi.length;
        Assert.assertArrayEquals(pi, receiverOutput.getPi());
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        // Δ = π(a) ⊕ b
        byte[][] a = senderOutput.getAs();
        byte[][] b = senderOutput.getBs();
        byte[][] expectDeltas = receiverOutput.getDeltas();
        byte[][] pa = PermutationNetworkUtils.permutation(pi, a);
        byte[][] actualDeltas = IntStream.range(0, num)
            .mapToObj(i -> BytesUtils.xor(pa[i], b[i]))
            .toArray(byte[][]::new);
        IntStream.range(0, num).forEach(i -> Assert.assertArrayEquals(expectDeltas[i], actualDeltas[i]));
    }
}
