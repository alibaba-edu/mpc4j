package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * (F3, F2)-wPRF test.
 *
 * @author Weiran Liu
 * @date 2024/5/24
 */
public class F32WprfTest {
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * PRF
     */
    private final F32Wprf f32Wprf;

    public F32WprfTest() {
        z3Field = new Z3ByteField();
        secureRandom = new SecureRandom();
        byte[] seedA = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        byte[] seedB = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        f32Wprf = new F32Wprf(z3Field, seedA, seedB);
    }

    @Test
    public void testPrf() {
        int inputLength = F32Wprf.getInputLength();
        int outputByteLength = F32Wprf.getOutputByteLength();
        byte[] key1, key2;
        byte[] input1, input2;
        byte[] output1, output2;
        // fix key and input
        key1 = f32Wprf.keyGen(secureRandom);
        key2 = BytesUtils.clone(key1);
        input1 = z3Field.createRandoms(inputLength, secureRandom);
        input2 = Arrays.copyOf(input1, input1.length);
        output1 = f32Wprf.prf(key1, input1);
        output2 = f32Wprf.prf(key2, input2);
        Assert.assertEquals(outputByteLength, output1.length);
        Assert.assertEquals(outputByteLength, output2.length);
        // two results are equal
        Assert.assertEquals(ByteBuffer.wrap(output1), ByteBuffer.wrap(output2));

        // fix key but change input
        key1 = f32Wprf.keyGen(secureRandom);
        key2 = BytesUtils.clone(key1);
        input1 = z3Field.createRandoms(inputLength, secureRandom);
        input2 = z3Field.createRandoms(inputLength, secureRandom);
        output1 = f32Wprf.prf(key1, input1);
        output2 = f32Wprf.prf(key2, input2);
        Assert.assertEquals(outputByteLength, output1.length);
        Assert.assertEquals(outputByteLength, output2.length);
        // two results are not equal
        Assert.assertNotEquals(ByteBuffer.wrap(output1), ByteBuffer.wrap(output2));

        // fix input but change key
        key1 = f32Wprf.keyGen(secureRandom);
        key2 = f32Wprf.keyGen(secureRandom);
        input1 = z3Field.createRandoms(inputLength, secureRandom);
        input2 = Arrays.copyOf(input1, input1.length);
        output1 = f32Wprf.prf(key1, input1);
        output2 = f32Wprf.prf(key2, input2);
        Assert.assertEquals(outputByteLength, output1.length);
        Assert.assertEquals(outputByteLength, output2.length);
        // two results are not equal
        Assert.assertNotEquals(ByteBuffer.wrap(output1), ByteBuffer.wrap(output2));
    }
}
