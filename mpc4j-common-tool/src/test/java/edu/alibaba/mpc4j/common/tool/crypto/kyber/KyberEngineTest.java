package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngineFactory.KyberType;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberKeyPair;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Kyber引擎测试。
 *
 * @author Sheng Hu
 * @date 2021/12/31
 */
@RunWith(Parameterized.class)
public class KyberEngineTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 400;
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 1 << 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // KYBER_CPA (k = 2)
        configurations.add(new Object[]{KyberType.KYBER_CPA + " (k = 2)", KyberType.KYBER_CPA, 2});
        // KYBER_CPA (k = 3)
        configurations.add(new Object[]{KyberType.KYBER_CPA + " (k = 3)", KyberType.KYBER_CPA, 3});
        // KYBER_CPA (k = 4)
        configurations.add(new Object[]{KyberType.KYBER_CPA + " (k = 4)", KyberType.KYBER_CPA, 4});
        // KYBER_CCA (k = 2)
        configurations.add(new Object[]{KyberType.KYBER_CCA + " (k = 2)", KyberType.KYBER_CPA, 2});
        // KYBER_CCA (k = 3)
        configurations.add(new Object[]{KyberType.KYBER_CCA + " (k = 3)", KyberType.KYBER_CPA, 3});
        // KYBER_CCA (k = 4)
        configurations.add(new Object[]{KyberType.KYBER_CCA + " (k = 4)", KyberType.KYBER_CPA, 4});

        return configurations;
    }

    /**
     * 待测试的Kyber类型
     */
    private final KyberType kyberType;
    /**
     * k
     */
    private final int paramsK;

    public KyberEngineTest(String name, KyberType kyberType, int paramsK) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.kyberType = kyberType;
        this.paramsK = paramsK;
    }

    @Test
    public void testType() {
        KyberEngine kyberEngine = KyberEngineFactory.createInstance(kyberType, paramsK);
        Assert.assertEquals(kyberType, kyberEngine.getKyberType());
    }

    @Test
    public void testEncapsulation() {
        KyberEngine kyberEngine = KyberEngineFactory.createInstance(kyberType, paramsK);
        int keyByteLength = kyberEngine.keyByteLength();
        KyberKeyPair keyPair = kyberEngine.generateKeyPair();
        byte[] publicKey = keyPair.getPublicKey();
        byte[] matrixSeed = keyPair.getMatrixSeed();
        short[][] secretKey = keyPair.getSecretKey();
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] encapsulateKey = new byte[keyByteLength];
            byte[] ciphertext = kyberEngine.encapsulate(encapsulateKey, publicKey, matrixSeed);
            byte[] decapsulateKey = kyberEngine.decapsulate(ciphertext, secretKey, publicKey, matrixSeed);
            Assert.assertArrayEquals(encapsulateKey, decapsulateKey);
        }
    }

    @Test
    public void testParallel() {
        KyberEngine kyberEngine = KyberEngineFactory.createInstance(kyberType, paramsK);
        int keyByteLength = kyberEngine.keyByteLength();
        KyberKeyPair keyPair = kyberEngine.generateKeyPair();
        byte[] publicKey = keyPair.getPublicKey();
        byte[] matrixSeed = keyPair.getMatrixSeed();
        short[][] secretKey = keyPair.getSecretKey();
        IntStream.range(0, MAX_PARALLEL).parallel().forEach(index -> {
            byte[] encapsulateKey = new byte[keyByteLength];
            byte[] ciphertext = kyberEngine.encapsulate(encapsulateKey, publicKey, matrixSeed);
            byte[] decapsulateKey = kyberEngine.decapsulate(ciphertext, secretKey, publicKey, matrixSeed);
            Assert.assertArrayEquals(encapsulateKey, decapsulateKey);
        });
    }
}
