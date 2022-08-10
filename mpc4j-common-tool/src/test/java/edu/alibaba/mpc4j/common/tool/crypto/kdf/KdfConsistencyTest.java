package edu.alibaba.mpc4j.common.tool.crypto.kdf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory.KdfType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 密钥派生函数一致性测试。
 *
 * @author Weiran Liu
 * @date 2022/01/07
 */
@RunWith(Parameterized.class)
@Ignore
public class KdfConsistencyTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 100;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // BLAKE_2B
        configurationParams.add(new Object[] {"Blake2b", KdfType.BC_BLAKE_2B, KdfType.NATIVE_BLAKE_2B});
        // SHA256
        configurationParams.add(new Object[] {"Sha256", KdfType.JDK_SHA256, KdfType.NATIVE_SHA256});

        return configurationParams;
    }

    /**
     * JDK类型
     */
    private final KdfType jdkType;
    /**
     * 本地类型
     */
    private final KdfType nativeType;

    public KdfConsistencyTest(String name, KdfType jdkType, KdfType nativeType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.jdkType = jdkType;
        this.nativeType = nativeType;
    }

    @Test
    public void testConsistency() {
        testConsistency(CommonConstants.BLOCK_BYTE_LENGTH);
        testConsistency(2 * CommonConstants.BLOCK_BYTE_LENGTH);
        testConsistency(4 * CommonConstants.BLOCK_BYTE_LENGTH);
        testConsistency(8 * CommonConstants.BLOCK_BYTE_LENGTH);
        testConsistency(16 * CommonConstants.BLOCK_BYTE_LENGTH);
    }

    private void testConsistency(int seedByteLength) {
        Kdf jdkKdf = KdfFactory.createInstance(jdkType);
        Kdf nativeKdf = KdfFactory.createInstance(nativeType);
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] seed = new byte[seedByteLength];
            SECURE_RANDOM.nextBytes(seed);
            byte[] jdkKey = jdkKdf.deriveKey(seed);
            byte[] nativeKey = nativeKdf.deriveKey(seed);
            Assert.assertArrayEquals(jdkKey, nativeKey);
        }
    }
}
