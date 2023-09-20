package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.*;

/**
 * Elligator byte multiplication ECC test.
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
@RunWith(Parameterized.class)
public class ByteMulElligatorEccTest {
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 1000;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * valid scalar
     */
    private static final byte[] CONSTANT_CLAMP_SCALAR = new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40,
    };

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            ByteEccFactory.ByteEccType.X25519_ELLIGATOR_BC.name(), ByteEccFactory.ByteEccType.X25519_ELLIGATOR_BC,
        });

        return configurations;
    }

    /**
     * type
     */
    private final ByteEccType byteEccType;

    public ByteMulElligatorEccTest(String name, ByteEccType byteEccType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.byteEccType = byteEccType;
    }

    @Test
    public void testConstantBaseMulElligator() {
        ByteMulElligatorEcc byteMulElligatorEcc = ByteEccFactory.createMulElligatorInstance(byteEccType);
        int pointByteLength = byteMulElligatorEcc.pointByteLength();
        // g^{r1}
        byte[] gr1 = new byte[pointByteLength];
        byte[] encodeGr1 = new byte[pointByteLength];
        Assert.assertTrue(byteMulElligatorEcc.baseMul(CONSTANT_CLAMP_SCALAR, gr1, encodeGr1));
        // g^{r2}
        byte[] gr2 = new byte[pointByteLength];
        byte[] encodeGr2 = new byte[pointByteLength];
        Assert.assertTrue(byteMulElligatorEcc.baseMul(CONSTANT_CLAMP_SCALAR, gr2, encodeGr2));
        byte[] gr12 = byteMulElligatorEcc.mul(gr1, CONSTANT_CLAMP_SCALAR);
        byte[] gr21 = byteMulElligatorEcc.mul(gr2, CONSTANT_CLAMP_SCALAR);
        Assert.assertArrayEquals(gr12, gr21);
        gr12 = byteMulElligatorEcc.uniformMul(encodeGr1, CONSTANT_CLAMP_SCALAR);
        gr21 = byteMulElligatorEcc.uniformMul(encodeGr2, CONSTANT_CLAMP_SCALAR);
        Assert.assertArrayEquals(gr12, gr21);
        gr12 = byteMulElligatorEcc.mul(gr1, CONSTANT_CLAMP_SCALAR);
        gr21 = byteMulElligatorEcc.uniformMul(encodeGr2, CONSTANT_CLAMP_SCALAR);
        Assert.assertArrayEquals(gr12, gr21);
        gr12 = byteMulElligatorEcc.uniformMul(encodeGr1, CONSTANT_CLAMP_SCALAR);
        gr21 = byteMulElligatorEcc.mul(gr2, CONSTANT_CLAMP_SCALAR);
        Assert.assertArrayEquals(gr12, gr21);
    }

    @Test
    public void testRandomBaseMulElligator() {
        ByteMulElligatorEcc byteMulElligatorEcc = ByteEccFactory.createMulElligatorInstance(byteEccType);
        int scalarByteLength = byteMulElligatorEcc.scalarByteLength();
        int pointByteLength = byteMulElligatorEcc.pointByteLength();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            boolean r1Success = false;
            byte[] r1 = new byte[scalarByteLength];
            byte[] gr1 = new byte[pointByteLength];
            byte[] encodeGr1 = new byte[pointByteLength];
            while (!r1Success) {
                // g^{r1}
                r1 = byteMulElligatorEcc.randomScalar(SECURE_RANDOM);
                r1Success = byteMulElligatorEcc.baseMul(r1, gr1, encodeGr1);
            }
            boolean r2Success = false;
            byte[] r2 = new byte[scalarByteLength];
            byte[] gr2 = new byte[pointByteLength];
            byte[] encodeGr2 = new byte[pointByteLength];
            while (!r2Success) {
                // g^{r2}
                r2 = byteMulElligatorEcc.randomScalar(SECURE_RANDOM);
                r2Success = byteMulElligatorEcc.baseMul(r2, gr2, encodeGr2);
            }
            byte[] gr12 = byteMulElligatorEcc.mul(gr1, r2);
            byte[] gr21 = byteMulElligatorEcc.mul(gr2, r1);
            Assert.assertArrayEquals(gr12, gr21);
            gr12 = byteMulElligatorEcc.uniformMul(encodeGr1, r2);
            gr21 = byteMulElligatorEcc.uniformMul(encodeGr2, r1);
            Assert.assertArrayEquals(gr12, gr21);
            gr12 = byteMulElligatorEcc.mul(gr1, r2);
            gr21 = byteMulElligatorEcc.uniformMul(encodeGr2, r1);
            Assert.assertArrayEquals(gr12, gr21);
            gr12 = byteMulElligatorEcc.uniformMul(encodeGr1, r2);
            gr21 = byteMulElligatorEcc.mul(gr2, r1);
            Assert.assertArrayEquals(gr12, gr21);
        }
    }
}
