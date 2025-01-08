package edu.alibaba.mpc4j.crypto.algs.smprp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.algs.smprp.SmallDomainPrpFactory.SmallDomainPrpType;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * small-domain PRP test.
 *
 * @author Weiran Liu
 * @date 2024/8/22
 */
@RunWith(Parameterized.class)
public class SmallDomainPrpTest {
    /**
     * parallel num
     */
    private static final int PARALLEL_NUM = 1 << 10;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // AD_PRP
        configurations.add(new Object[]{SmallDomainPrpType.AD_PRP.name(), SmallDomainPrpType.AD_PRP,});

        return configurations;
    }

    /**
     * type
     */
    public final SmallDomainPrpType type;

    public SmallDomainPrpTest(String name, SmallDomainPrpType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        SmallDomainPrp prp = SmallDomainPrpFactory.createInstance(EnvType.STANDARD, type);
        // set short key
        Assert.assertThrows(AssertionError.class, () -> prp.init(10, new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]));
        // set long key
        Assert.assertThrows(AssertionError.class, () -> prp.init(10, new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]));

        byte[] key = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        // set negative range
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.init(-1, key));
        // set 0 range
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.init(0, key));
        // execute without init
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.prp(5));
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.invPrp(5));

        int range = 10;
        prp.init(range, key);
        // negative plaintext and ciphertext
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.prp(-1));
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.invPrp(-1));
        // range plaintext and ciphertext
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.prp(range));
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.invPrp(range));
        // large plaintext and ciphertext
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.prp(range + 1));
        Assert.assertThrows(IllegalArgumentException.class, () -> prp.invPrp(range + 1));
    }

    @Test
    public void testType() {
        SmallDomainPrp smallDomainPrp = SmallDomainPrpFactory.createInstance(EnvType.STANDARD, type);
        Assert.assertEquals(type, smallDomainPrp.getType());
    }

    @Test
    public void testConstant() {
        // range = [0, 1)
        testConstant(1);
        // range = [0, 3)
        testConstant(3);
        // range = [0, 2^3)
        testConstant(1 << 3);
        // range = [0, 2^16 - 1)
        testConstant((1 << 16) - 1);
        // range = [0, 2^16)
        testConstant(1 << 16);
        // range = [0, 2^16)
        testConstant((1 << 16) + 1);
    }

    private void testConstant(int range) {
        SmallDomainPrp prp = SmallDomainPrpFactory.createInstance(EnvType.STANDARD, type);
        byte[] key = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        prp.init(range, key);
        // test PRP
        int[] ciphertexts = new int[range];
        TIntSet ciphertextSet = new TIntHashSet(range);
        for (int i = 0; i < range; i++) {
            // invoke first time
            ciphertexts[i] = prp.prp(i);
            Assert.assertTrue(ciphertexts[i] >= 0 && ciphertexts[i] < range);
            ciphertextSet.add(ciphertexts[i]);
            // invoke second time
            Assert.assertEquals(ciphertexts[i], prp.prp(i));
        }
        Assert.assertEquals(range, ciphertextSet.size());
        // test inverse PRP
        for (int i = 0; i < range; i++) {
            // invoke first time
            Assert.assertEquals(i, prp.invPrp(ciphertexts[i]));
            // invoke second time
            Assert.assertEquals(i, prp.invPrp(ciphertexts[i]));
        }
    }

    @Test
    public void testRandom() {
        // [0, 2^10 - 1)
        testRandomKey((1 << 10) - 1);
        // [0, 2^10)
        testRandomKey(1 << 10);
        // [0, 2^10 + 1)
        testRandomKey((1 << 10) + 1);
    }

    private void testRandomKey(int range) {
        SmallDomainPrp prp = SmallDomainPrpFactory.createInstance(EnvType.STANDARD, type);
        // key1
        int[] result1 = new int[range];
        byte[] key1 = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        prp.init(range, key1);
        for (int i = 0; i < range; i++) {
            result1[i] = prp.prp(i);
            Assert.assertEquals(i, prp.invPrp(result1[i]));
        }
        // key2
        int[] result2 = new int[range];
        byte[] key2 = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        prp.init(range, key2);
        for (int i = 0; i < range; i++) {
            result2[i] = prp.prp(i);
            Assert.assertEquals(i, prp.invPrp(result2[i]));
        }
        // verify
        Assert.assertFalse(Arrays.equals(result1, result2));
    }

    @Test
    public void testParallel() {
        SmallDomainPrp prp = SmallDomainPrpFactory.createInstance(EnvType.STANDARD, type);
        byte[] key = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        prp.init(1 << 10, key);
        // PRP
        Set<Integer> ciphertextSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .map(index -> prp.prp(0))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(1, ciphertextSet.size());
        // inverse PRP
        Set<Integer> plaintextSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .map(index -> prp.invPrp(0))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(1, plaintextSet.size());
    }
}
