package edu.alibaba.mpc4j.common.tool.lpn.ldpc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;
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

import static edu.alibaba.mpc4j.common.tool.CommonConstants.BLOCK_BYTE_LENGTH;
import static edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorUtils.CodeType.SILVER_11;
import static edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorUtils.CodeType.SILVER_5;

/**
 * LdpcCoder 测试类，测试Ldpc Coder提供的转置编码方法。
 * LdpcCoder的用途是将一组稀疏的COT转换为一组伪随机的COT。
 * 稀疏的COT由初始随机消息、初始获得消息、初始选择比特和\Delta 构成；对他们分别进行转置编码后，应该得到扩展后的随机消息、获得消息和选择比特。
 * 仍然构成一组COT。
 *
 * @author Hanwen Feng
 * @date 2022/03/21
 */
@RunWith(Parameterized.class)
public class LdpcCoderTest {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LdpcCoderTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("000.000");

    /**
     * 设置测试参数
     *
     * @return 设置参数
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // 设置输出OT数量的对数 12 ~ 19。
        for (int ceilLogN = 12; ceilLogN < 20; ceilLogN++) {
            configurationParams.add(new Object[] {"Silver5, ceilLogN = " + ceilLogN, SILVER_5, ceilLogN});
            configurationParams.add(new Object[] {"Silver11, ceilLogN = " + ceilLogN, SILVER_11, ceilLogN});
        }
        return configurationParams;
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
     * 编码器
     */
    private final LdpcCoder encoder;
    /**
     * Ldpc 类型
     */
    private final LdpcCreatorUtils.CodeType codeType;

    /**
     * 构造函数
     *
     * @param name     配置名称
     * @param codeType Ldpc类型
     * @param ceilLogN 目标输出OT数量的对数
     */
    public LdpcCoderTest(String name, LdpcCreatorUtils.CodeType codeType, int ceilLogN) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.codeType = codeType;
        // 根据输入，创建creator。
        LdpcCreator creator = LdpcCreatorFactory.createLdpcCreator(codeType, ceilLogN);
        // 从creator中读取当前的LPN参数。
        LpnParams lpnParams = creator.getLpnParams();
        init(lpnParams);
        encoder = creator.createLdpcCoder();
    }

    @Test
    public void testEfficiency() {
        encoder.setParallel(false);
        testEncoderEfficiency();
    }

    @Test
    public void testParallelEfficiency() {
        encoder.setParallel(true);
        testEncoderEfficiency();
    }

    private void testEncoderEfficiency() {
        // 预热。
        encoder.transEncode(initRandomMsgs);
        StopWatch stopwatch = new StopWatch();

        // 测试对byte[][] 类型的时间。
        stopwatch.start();
        byte[][] extendRandomMsgs = encoder.transEncode(initRandomMsgs);
        stopwatch.stop();
        double bytesEncodeTime = (double)stopwatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopwatch.reset();
        // 扩招initGetMsgs。
        byte[][] extendGetMsgs = encoder.transEncode(initGetMsgs);
        // 测试对boolean 的时间。
        stopwatch.start();
        boolean[] extendChoiceBits = encoder.transEncode(initChoiceBits);
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
        int gapValue = LdpcCreatorUtils.getGap(codeType);
        int kValue = lpnParams.getK();
        int dimension = 2 * kValue - gapValue;
        int errorWeight = lpnParams.getT();

        initRandomMsgs = new byte[dimension][BLOCK_BYTE_LENGTH];
        deltaValue = new byte[BLOCK_BYTE_LENGTH];
        initChoiceBits = new boolean[dimension];
        initGetMsgs = new byte[dimension][BLOCK_BYTE_LENGTH];
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
