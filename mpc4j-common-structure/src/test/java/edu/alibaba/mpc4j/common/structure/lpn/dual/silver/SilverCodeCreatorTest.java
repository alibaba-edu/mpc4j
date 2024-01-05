package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
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

/**
 * LdpcCreator 测试类， 测试 FullLdpcCreator 和 OnlineLdpcCreator 两个实现。
 *
 * @author Hanwen Feng
 * @date 2022/03/15
 */
@RunWith(Parameterized.class)
public class SilverCodeCreatorTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int ceilLogN = 12; ceilLogN < 16; ceilLogN++) {
            configurations.add(
                new Object[] {" ceilLogN = " + ceilLogN + ", codeType = Silver5", ceilLogN, SilverCodeType.SILVER_5});
            configurations.add(
                new Object[] {" ceilLogN = " + ceilLogN + ", codeType = Silver11", ceilLogN, SilverCodeType.SILVER_11});
        }

        return configurations;
    }

    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("000.000");
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SilverCodeCreatorTest.class);
    /**
     * 目标输出OT数量的对数
     */
    private final int ceilLogN;
    /**
     * Ldpc类型
     */
    private final SilverCodeType silverCodeType;

    /**
     * 构造函数
     *
     * @param name     参数配置名称
     * @param ceilLogN 目标输出OT数量的对数
     * @param silverCodeType Ldpc类型
     */
    public SilverCodeCreatorTest(String name, int ceilLogN, SilverCodeType silverCodeType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.ceilLogN = ceilLogN;
        this.silverCodeType = silverCodeType;
    }

    /**
     * 测试Creator构造时间
     */
    @Test
    public void testCreatorEfficiency() {
        // 预热
        SilverCodeCreatorFactory.createInstance(silverCodeType, ceilLogN);

        StopWatch stopWatch = new StopWatch();
        // 测试在线生成ldpc所需时间。
        stopWatch.start();
        SilverCodeCreatorFactory.createInstance(silverCodeType, ceilLogN);
        stopWatch.stop();
        double codeGenTime = (double)stopWatch.getTime(TimeUnit.MICROSECONDS) / 1000;
        stopWatch.reset();

        LOGGER.info("ceilLogN: \t{} , CodeType: \t{}, Time. \t{} ms", ceilLogN, silverCodeType.name(),
            TIME_DECIMAL_FORMAT.format(codeGenTime));
    }

}
