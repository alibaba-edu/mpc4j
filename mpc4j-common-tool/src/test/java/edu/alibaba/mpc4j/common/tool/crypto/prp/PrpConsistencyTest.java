package edu.alibaba.mpc4j.common.tool.crypto.prp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
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
 * PRP一致性测试。
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
@RunWith(Parameterized.class)
@Ignore
public class PrpConsistencyTest {
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
        // AES
        configurationParams.add(new Object[] {"AES", PrpType.JDK_AES, PrpType.NATIVE_AES});
        // LOW_MC_20
        configurationParams.add(new Object[] {"LOW_MC_20", PrpType.JDK_BYTES_LOW_MC_20, PrpType.JDK_LONGS_LOW_MC_20});
        // LOW_MC_21
        configurationParams.add(new Object[] {"LOW_MC_21", PrpType.JDK_BYTES_LOW_MC_21, PrpType.JDK_LONGS_LOW_MC_21});
        // LOW_MC_23
        configurationParams.add(new Object[] {"LOW_MC_23", PrpType.JDK_BYTES_LOW_MC_23, PrpType.JDK_LONGS_LOW_MC_23});
        // LOW_MC_32
        configurationParams.add(new Object[] {"LOW_MC_32", PrpType.JDK_BYTES_LOW_MC_32, PrpType.JDK_LONGS_LOW_MC_32});
        // LOW_MC_192
        configurationParams.add(new Object[] {"LOW_MC_192", PrpType.JDK_BYTES_LOW_MC_192, PrpType.JDK_LONGS_LOW_MC_192});
        // LOW_MC_208
        configurationParams.add(new Object[] {"LOW_MC_208", PrpType.JDK_BYTES_LOW_MC_208, PrpType.JDK_LONGS_LOW_MC_208});
        // LOW_MC_287
        configurationParams.add(new Object[] {"LOW_MC_287", PrpType.JDK_BYTES_LOW_MC_287, PrpType.JDK_LONGS_LOW_MC_287});

        return configurationParams;
    }

    /**
     * 被比较类型
     */
    private final PrpType thisType;
    /**
     * 比较类型
     */
    private final PrpType thatType;

    public PrpConsistencyTest(String name, PrpType thisType, PrpType thatType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.thisType = thisType;
        this.thatType = thatType;
    }

    @Test
    public void testPrpConsistency() {
        Prp thisPrp = PrpFactory.createInstance(thisType);
        Prp thatPrp = PrpFactory.createInstance(thatType);
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(key);
        thisPrp.setKey(key);
        thatPrp.setKey(key);
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(message);
            byte[] thisResult = thisPrp.prp(message);
            byte[] thatResult = thatPrp.prp(message);
            Assert.assertArrayEquals(thisResult, thatResult);
        }
    }

    @Test
    public void testInvPrpConsistency() {
        Prp thisPrp = PrpFactory.createInstance(thisType);
        Prp thatPrp = PrpFactory.createInstance(thatType);
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(key);
        thisPrp.setKey(key);
        thatPrp.setKey(key);
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(message);
            byte[] thisResult = thisPrp.invPrp(message);
            byte[] thatResult = thatPrp.invPrp(message);
            Assert.assertArrayEquals(thisResult, thatResult);
        }
    }
}
