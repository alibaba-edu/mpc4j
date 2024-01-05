package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Silver Coder test.
 *
 * @author Hanwen Feng
 * @date 2022/3/21
 */
@RunWith(Parameterized.class)
public class SilverCoderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SilverCoderTest.class);
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("000.000");

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int ceilLogN = 12; ceilLogN < 20; ceilLogN++) {
            configurations.add(new Object[] {"Silver5, ceilLogN = " + ceilLogN, SilverCodeType.SILVER_5, ceilLogN});
            configurations.add(new Object[] {"Silver11, ceilLogN = " + ceilLogN, SilverCodeType.SILVER_11, ceilLogN});
        }
        return configurations;
    }

    /**
     * 初始随机消息
     */
    private byte[][] initRandomMsgs;
    /**
     * Delta值
     */
    private byte[] deltaValue;
    /**
     * 初始选择比特
     */
    public boolean[] initChoiceBits;
    /**
     * 初始获得消息
     */
    public byte[][] initGetMsgs;
    /**
     * Silver Coder
     */
    private final SilverCoder silverCoder;
    /**
     * Silver Code type
     */
    private final SilverCodeType silverCodeType;

    public SilverCoderTest(String name, SilverCodeType silverCodeType, int ceilLogN) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.silverCodeType = silverCodeType;
        // 根据输入，创建creator。
        SilverCodeCreator creator = SilverCodeCreatorFactory.createInstance(silverCodeType, ceilLogN);
        // 从creator中读取当前的LPN参数。
        LpnParams lpnParams = creator.getLpnParams();
        init(lpnParams);
        silverCoder = creator.createCoder();
    }

    @Test
    public void testEfficiency() {
        silverCoder.setParallel(false);
        testEncoderEfficiency();
    }

    @Test
    public void testParallelEfficiency() {
        silverCoder.setParallel(true);
        testEncoderEfficiency();
    }

    private void testEncoderEfficiency() {
        // 预热。
        silverCoder.dualEncode(initRandomMsgs);
        StopWatch stopwatch = new StopWatch();

        // 测试对byte[][] 类型的时间。
        stopwatch.start();
        byte[][] extendRandomMsgs = silverCoder.dualEncode(initRandomMsgs);
        stopwatch.stop();
        double bytesEncodeTime = (double)stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();
        // 扩招initGetMsgs。
        byte[][] extendGetMsgs = silverCoder.dualEncode(initGetMsgs);
        // 测试对boolean 的时间。
        stopwatch.start();
        boolean[] extendChoiceBits = silverCoder.dualEncode(initChoiceBits);
        stopwatch.stop();
        double booleanEncodeTime = (double)stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();
        // 验证输出是否构成一组COT。
        assert isCOT(extendGetMsgs, extendRandomMsgs, extendChoiceBits);

        LOGGER.info("Input Type        \tTime(ms)");
        LOGGER.info("Boolean Array .\t{}", TIME_DECIMAL_FORMAT.format(booleanEncodeTime));
        LOGGER.info("Byte Arrays .\t{}", TIME_DECIMAL_FORMAT.format(bytesEncodeTime));
    }

    /**
     * 初始化系统参数
     *
     * @param lpnParams Lpn 参数
     */
    private void init(LpnParams lpnParams) {
        int gapValue = SilverCodeCreatorUtils.getGap(silverCodeType);
        int kValue = lpnParams.getK();
        int dimension = 2 * kValue - gapValue;
        int errorWeight = lpnParams.getT();

        initRandomMsgs = new byte[dimension][CommonConstants.BLOCK_BYTE_LENGTH];
        deltaValue = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        initChoiceBits = new boolean[dimension];
        initGetMsgs = new byte[dimension][CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(deltaValue);

        int[] errorPositions = new int[errorWeight];
        for (int i = 0; i < errorWeight; i++) {
            errorPositions[i] = SECURE_RANDOM.nextInt(dimension);
            initChoiceBits[errorPositions[i]] = true;
        }
        for (int i = 0; i < dimension; i++) {
            SECURE_RANDOM.nextBytes(initRandomMsgs[i]);
            if (initChoiceBits[i]) {
                initGetMsgs[i] = BytesUtils.xor(initRandomMsgs[i], deltaValue);
            } else {
                initGetMsgs[i] = BytesUtils.clone(initRandomMsgs[i]);
            }
        }
    }

    /**
     * 判断输入是否构成一组COT
     *
     * @param extendGetMsgs    Receiver获得的消息
     * @param extendRandomMsgs Sender的随机消息
     * @param extendChoiceBits Receiver的选择比特串
     * @return 若构成COT，则返回true
     */
    private boolean isCOT(byte[][] extendGetMsgs, byte[][] extendRandomMsgs, boolean[] extendChoiceBits) {
        for (int i = 0; i < extendGetMsgs.length; i++) {
            if (extendChoiceBits[i]) {
                if (!Arrays.equals(extendGetMsgs[i], BytesUtils.xor(extendRandomMsgs[i], deltaValue))) {
                    return false;
                }
            } else {
                if (!Arrays.equals(extendGetMsgs[i], extendRandomMsgs[i])) {
                    return false;
                }
            }
        }
        return true;
    }
}
