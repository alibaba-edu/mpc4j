package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23WprfMatrixFactory.F23WprfMatrixType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * F2 -> F3 weak PRF test.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
@RunWith(Parameterized.class)
public class F23WprfTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 1000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{F23WprfMatrixType.NAIVE});
        configurations.add(new Object[]{F23WprfMatrixType.BYTE});
        configurations.add(new Object[]{F23WprfMatrixType.LONG});

        return configurations;
    }

    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * weak PRF
     */
    private final F23Wprf wprf;

    public F23WprfTest(F23WprfMatrixType type) {
        Z3ByteField z3Field = new Z3ByteField();
        secureRandom = new SecureRandom();
        byte[] seedA = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        byte[] seedB = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        wprf = new F23Wprf(z3Field, seedA, seedB, type);
    }

    @Test
    public void testPrf() {
        int inputByteLength = F23Wprf.getInputByteLength();
        int outputLength = F23Wprf.getOutputLength();
        byte[] key1, key2;
        byte[] input1, input2, output1, output2;
        // fix key and input
        key1 = wprf.keyGen(secureRandom);
        input1 = BytesUtils.randomByteArray(inputByteLength, secureRandom);
        wprf.init(key1);
        output1 = wprf.prf(input1);
        Assert.assertEquals(outputLength, output1.length);
        // repeat and get same result
        input2 = BytesUtils.clone(input1);
        output2 = wprf.prf(input2);
        Assert.assertEquals(ByteBuffer.wrap(output1), ByteBuffer.wrap(output2));
        // fix key but change input
        input2 = BytesUtils.randomByteArray(inputByteLength, secureRandom);
        output2 = wprf.prf(input2);
        Assert.assertEquals(outputLength, output2.length);
        Assert.assertNotEquals(ByteBuffer.wrap(output1), ByteBuffer.wrap(output2));
        // fix input but change key
        key2 = wprf.keyGen(secureRandom);
        wprf.init(key2);
        output2 = wprf.prf(input1);
        Assert.assertEquals(outputLength, output2.length);
        Assert.assertNotEquals(ByteBuffer.wrap(output1), ByteBuffer.wrap(output2));
    }

    @Test
    public void testRandom() {
        byte[] key = wprf.keyGen(secureRandom);
        // same key, same inputs
        byte[] input = BytesUtils.randomByteArray(F23Wprf.getInputByteLength(), secureRandom);
        wprf.init(key);
        Set<ByteBuffer> set = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(i -> wprf.prf(input))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, set.size());
        // same key, random inputs
        set = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(i -> BytesUtils.randomByteArray(F23Wprf.getInputByteLength(), secureRandom))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(RANDOM_ROUND, set.size());
        // different key, same input
        set = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(i -> {
                byte[] randomKey = wprf.keyGen(secureRandom);
                wprf.init(randomKey);
                return wprf.prf(input);
            })
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(RANDOM_ROUND, set.size());
    }
}
