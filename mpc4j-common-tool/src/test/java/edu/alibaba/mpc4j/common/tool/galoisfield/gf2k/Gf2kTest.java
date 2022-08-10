package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GF(2^128)功能测试。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
@RunWith(Parameterized.class)
public class Gf2kTest {
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // SSE
        configurationParams.add(new Object[]{Gf2kType.SSE.name(), Gf2kType.SSE,});
        // NTL
        configurationParams.add(new Object[]{Gf2kType.NTL.name(), Gf2kType.NTL,});
        // BC
        configurationParams.add(new Object[]{Gf2kType.BC.name(), Gf2kType.BC,});
        // RINGS
        configurationParams.add(new Object[]{Gf2kType.RINGS.name(), Gf2kType.RINGS,});

        return configurationParams;
    }

    /**
     * GF(2^128)运算类型
     */
    private final Gf2kType type;
    /**
     * GF(2^128)运算
     */
    private final Gf2k gf2k;
    /**
     * 有限域字节长度
     */
    private final int byteL;

    public Gf2kTest(String name, Gf2kType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        gf2k = Gf2kFactory.createInstance(type);
        byteL = gf2k.getByteL();
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, gf2k.getGf2kType());
    }

    @Test
    public void testIllegalInputs() {
        // 尝试对错误长度的a做运算
        try {
            byte[] a = new byte[byteL - 1];
            byte[] b = new byte[byteL];
            gf2k.add(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length a");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL - 1];
            byte[] b = new byte[byteL];
            gf2k.addi(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length a");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL - 1];
            byte[] b = new byte[byteL];
            gf2k.mul(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length a");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL - 1];
            byte[] b = new byte[byteL];
            gf2k.muli(a, b);
            throw new IllegalStateException("ERROR: successfully compute a * b for wrong-length a");
        } catch (AssertionError ignored) {

        }
        // 尝试对错误长度的b做运算
        try {
            byte[] a = new byte[byteL];
            byte[] b = new byte[byteL - 1];
            gf2k.add(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length b");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL];
            byte[] b = new byte[byteL - 1];
            gf2k.addi(a, b);
            throw new IllegalStateException("ERROR: successfully compute a + b for wrong-length b");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL];
            byte[] b = new byte[byteL - 1];
            gf2k.mul(a, b);
            throw new IllegalStateException("ERROR: successfully compute a * b for wrong-length b");
        } catch (AssertionError ignored) {

        }
        try {
            byte[] a = new byte[byteL];
            byte[] b = new byte[byteL - 1];
            gf2k.muli(a, b);
            throw new IllegalStateException("ERROR: successfully compute a * b for wrong-length b");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testMultiply() {
        // 0 * 0 = 0
        byte[] a = gf2k.createZero();
        byte[] b = gf2k.createZero();
        byte[] truth = gf2k.createZero();
        testMultiply(a, b, truth);
        // 1 * 1 = 1
        a = gf2k.createOne();
        b = gf2k.createOne();
        truth = gf2k.createOne();
        testMultiply(a, b, truth);
        // x * x = x^2
        a = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02
        };
        b = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02
        };
        truth = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04
        };
        testMultiply(a, b, truth);
        // x^2 * x^2 = x^4
        a = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04
        };
        b = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04
        };
        truth = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10
        };
        testMultiply(a, b, truth);
    }

    private void testMultiply(byte[] a, byte[] b, byte[] truth) {
        byte[] c = gf2k.mul(a, b);
        Assert.assertArrayEquals(truth, c);
        gf2k.muli(a, b);
        Assert.assertArrayEquals(truth, a);
    }


    @Test
    public void testParallel() {
        Set<ByteBuffer> cArray = IntStream.range(0, MAX_PARALLEL)
            .mapToObj(index -> {
                byte[] a = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                Arrays.fill(a, (byte) 0xFF);
                byte[] b = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                Arrays.fill(b, (byte) 0xFF);
                return gf2k.mul(a, b);
            }).map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, cArray.size());

        Set<ByteBuffer> aArray = IntStream.range(0, MAX_PARALLEL)
            .mapToObj(index -> {
                byte[] a = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                Arrays.fill(a, (byte) 0xFF);
                byte[] b = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                Arrays.fill(b, (byte) 0xFF);
                gf2k.muli(a, b);
                return a;
            }).map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, aArray.size());
    }
}
