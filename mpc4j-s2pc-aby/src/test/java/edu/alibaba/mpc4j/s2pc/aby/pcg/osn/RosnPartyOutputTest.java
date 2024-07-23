package edu.alibaba.mpc4j.s2pc.aby.pcg.osn;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Random OSN party output test.
 *
 * @author Weiran Liu
 * @date 2024/6/29
 */
@RunWith(Parameterized.class)
public class RosnPartyOutputTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 2;
    /**
     * max num
     */
    private static final int MAX_NUM = 64;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int byteL : new int[]{1, 2, 4, 8, 16, 32}) {
            configurations.add(new Object[]{"byteL = " + byteL, byteL});
        }

        return configurations;
    }

    /**
     * byteL
     */
    private final int byteL;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public RosnPartyOutputTest(String name, int byteL) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.byteL = byteL;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalSenderInputs() {
        // create a sender output with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] as = new byte[0][byteL];
            byte[][] bs = new byte[0][byteL];
            RosnSenderOutput.create(as, bs);
        });
        // create a sender output with num = 1
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] as = BytesUtils.randomByteArrayVector(1, byteL, secureRandom);
            byte[][] bs = BytesUtils.randomByteArrayVector(1, byteL, secureRandom);
            RosnSenderOutput.create(as, bs);
        });
        // create a sender output with mismatch num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] as = BytesUtils.randomByteArrayVector(MIN_NUM, byteL, secureRandom);
            byte[][] bs = BytesUtils.randomByteArrayVector(MAX_NUM, byteL, secureRandom);
            RosnSenderOutput.create(as, bs);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] as = BytesUtils.randomByteArrayVector(MAX_NUM, byteL, secureRandom);
            byte[][] bs = BytesUtils.randomByteArrayVector(MIN_NUM, byteL, secureRandom);
            RosnSenderOutput.create(as, bs);
        });
        // create a sender output with mismatch length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] as = BytesUtils.randomByteArrayVector(MAX_NUM, byteL, secureRandom);
            byte[][] bs = BytesUtils.randomByteArrayVector(MAX_NUM, byteL + 1, secureRandom);
            RosnSenderOutput.create(as, bs);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] as = BytesUtils.randomByteArrayVector(MAX_NUM, byteL + 1, secureRandom);
            byte[][] bs = BytesUtils.randomByteArrayVector(MAX_NUM, byteL, secureRandom);
            RosnSenderOutput.create(as, bs);
        });
    }

    @Test
    public void testIllegalReceiverOutputs() {
        // create a receiver output with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] pi = new int[0];
            byte[][] deltas = new byte[0][byteL];
            RosnReceiverOutput.create(pi, deltas);
        });
        Assert.assertThrows(IllegalArgumentException.class, () ->
            RosnReceiverOutput.createRandom(0, byteL, secureRandom)
        );
        // create a receiver output with num = 1
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] pi = new int[] {0};
            byte[][] deltas = BytesUtils.randomByteArrayVector(1, byteL, secureRandom);
            RosnReceiverOutput.create(pi, deltas);
        });
        Assert.assertThrows(IllegalArgumentException.class, () ->
            RosnReceiverOutput.createRandom(1, byteL, secureRandom)
        );
        // create a receiver output with wrong π
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] pi = new int[] {0, 0};
            byte[][] deltas = BytesUtils.randomByteArrayVector(pi.length, byteL, secureRandom);
            RosnReceiverOutput.create(pi, deltas);
        });
        // create a receiver output with mismatched num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] pi = PermutationNetworkUtils.randomPermutation(MIN_NUM, secureRandom);
            byte[][] deltas = BytesUtils.randomByteArrayVector(MAX_NUM, byteL, secureRandom);
            RosnReceiverOutput.create(pi, deltas);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] pi = PermutationNetworkUtils.randomPermutation(MAX_NUM, secureRandom);
            byte[][] deltas = BytesUtils.randomByteArrayVector(MIN_NUM, byteL, secureRandom);
            RosnReceiverOutput.create(pi, deltas);
        });
    }

    @Test
    public void testCreateRandom() {
        for (int num = MIN_NUM; num <= MAX_NUM; num++) {
            RosnReceiverOutput receiverOutput = RosnReceiverOutput.createRandom(num, byteL, secureRandom);
            RosnSenderOutput senderOutput = RosnSenderOutput.createRandom(receiverOutput, secureRandom);
            int[] pi = receiverOutput.getPi();
            OsnTestUtils.assertOutput(pi, senderOutput, receiverOutput);
        }
    }
}
