package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32WprfMatrixFactory.F32WprfMatrixType;
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
 * (F3, F2)-wPRF test.
 *
 * @author Weiran Liu
 * @date 2024/5/24
 */
@RunWith(Parameterized.class)
public class F32WprfTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 1000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{F32WprfMatrixType.NAIVE});
        configurations.add(new Object[]{F32WprfMatrixType.BYTE});
        configurations.add(new Object[]{F32WprfMatrixType.LONG});

        return configurations;
    }

    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * weak PRF
     */
    private final F32Wprf wprf;

    public F32WprfTest(F32WprfMatrixType type) {
        z3Field = new Z3ByteField();
        secureRandom = new SecureRandom();
        byte[] seedA = BlockUtils.randomBlock(secureRandom);
        byte[] seedB = BlockUtils.randomBlock(secureRandom);
        wprf = new F32Wprf(z3Field, seedA, seedB, type);
    }

    @Test
    public void testPrf() {
        int inputLength = F32Wprf.getInputLength();
        int outputByteLength = F32Wprf.getOutputByteLength();
        byte[] key1, key2;
        byte[] input1, input2, output1, output2;
        // fix key and input
        key1 = wprf.keyGen(secureRandom);
        input1 = z3Field.createRandoms(inputLength, secureRandom);
        wprf.init(key1);
        output1 = wprf.prf(input1);
        Assert.assertEquals(outputByteLength, output1.length);
        // repeat and get same result
        input2 = BytesUtils.clone(input1);
        output2 = wprf.prf(input2);
        Assert.assertEquals(ByteBuffer.wrap(output1), ByteBuffer.wrap(output2));
        // fix key but change input
        input2 = z3Field.createRandoms(inputLength, secureRandom);
        output2 = wprf.prf(input2);
        Assert.assertEquals(outputByteLength, output2.length);
        Assert.assertNotEquals(ByteBuffer.wrap(output1), ByteBuffer.wrap(output2));
        // fix input but change key
        key2 = wprf.keyGen(secureRandom);
        wprf.init(key2);
        output2 = wprf.prf(input1);
        Assert.assertEquals(outputByteLength, output2.length);
        Assert.assertNotEquals(ByteBuffer.wrap(output1), ByteBuffer.wrap(output2));
    }

    @Test
    public void testRandom() {
        byte[] key = wprf.keyGen(secureRandom);
        // same key, same inputs
        byte[] input = z3Field.createRandoms(F32Wprf.getInputLength(), secureRandom);
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
