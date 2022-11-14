package edu.alibaba.mpc4j.common.tool.lpn.ldpc;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorUtils.CodeType.SILVER_11;
import static edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorUtils.CodeType.SILVER_5;

/**
 * LdpcCreator 测试类， 测试 FullLdpcCreator 和 OnlineLdpcCreator 两个实现。
 *
 * @author Hanwen Feng
 * @date 2022/03/15
 */
@RunWith(Parameterized.class)
public class LdpcCreatorTest {
    /**
     * 设置测试参数
     *
     * @return 参数配置
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // 测试silver 5 和 Silver11 两类。12~13会调用FullLdpcCreator，14～15 会调用OnlineLdpcCreator。
        for (int ceilLogN = 12; ceilLogN < 16; ceilLogN++) {
            configurationParams.add(
                new Object[] {" ceilLogN = " + ceilLogN + ", codeType = Silver5", ceilLogN, SILVER_5});
            configurationParams.add(
                new Object[] {" ceilLogN = " + ceilLogN + ", codeType = Silver11", ceilLogN, SILVER_11});
        }
        return configurationParams;
    }

    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("000.000");
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LdpcCreatorTest.class);
    /**
     * 目标输出OT数量的对数
     */
    private final int ceilLogN;
    /**
     * Ldpc类型
     */
    private final LdpcCreatorUtils.CodeType codeType;

    /**
     * 构造函数
     *
     * @param name     参数配置名称
     * @param ceilLogN 目标输出OT数量的对数
     * @param codeType Ldpc类型
     */
    public LdpcCreatorTest(String name, int ceilLogN, LdpcCreatorUtils.CodeType codeType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.ceilLogN = ceilLogN;
        this.codeType = codeType;
    }

    /**
     * 测试Creator构造时间
     */
    @Test
    public void testCreatorEfficiency() {
        // 预热
        LdpcCreatorFactory.createLdpcCreator(codeType, ceilLogN);

        StopWatch stopWatch = new StopWatch();
        // 测试在线生成ldpc所需时间。
        stopWatch.start();
        LdpcCreatorFactory.createLdpcCreator(codeType, ceilLogN);
        stopWatch.stop();
        double codeGenTime = (double)stopWatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopWatch.reset();

        LOGGER.info("ceilLogN: \t{} , CodeType: \t{}, Time. \t{} ms", ceilLogN, codeType.name(),
            TIME_DECIMAL_FORMAT.format(codeGenTime));
    }

}
