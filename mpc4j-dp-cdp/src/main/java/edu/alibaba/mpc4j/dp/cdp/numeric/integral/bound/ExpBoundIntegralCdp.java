package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 指数有界整数CDP机制。
 * <p>
 * 传统指数机制中的Δq设置为分数值最大变化量，这里要设置为Δq = 1，因此不能直接通过指数机制构建有界距离差分隐私机制。
 * </p>
 * <p>
 * 这里的实现方法为：行为输入，列为输出，构建输入与输出的累计概率密度函数，应用累计概率密度函数得到随机化结果。
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/24
 */
class ExpBoundIntegralCdp implements BoundIntegralCdp {
    /**
     * 配置项
     */
    private ExpBoundIntegralCdpConfig boundIntegralCdpConfig;
    /**
     * 下界
     */
    private int lowerBound;
    /**
     * 上界
     */
    private int upperBound;
    /**
     * 归一化上界，将[lowerBound, upperBound]归一化为[0, upperBound - lowerBound]
     */
    private int bound;
    /**
     * 为输入构建的累计概率密度函数映射二维数组，数组的长度和宽度均为bound
     */
    private double[][] cdfTable;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof ExpBoundIntegralCdpConfig;
        boundIntegralCdpConfig = (ExpBoundIntegralCdpConfig) cdpConfig;
        lowerBound = boundIntegralCdpConfig.getLowerBound();
        upperBound = boundIntegralCdpConfig.getUpperBound();
        // 设置bound
        bound = upperBound - lowerBound + 1;
        // 按行计算距离值所对应的系数
        double epsilon = boundIntegralCdpConfig.getBaseEpsilon();
        // 构建概率因子二维数组
        cdfTable = IntStream.range(0, bound)
            .mapToObj(input -> IntStream.range(0, bound)
                .mapToDouble(output -> Math.exp((-1.0 * Math.abs(input - output) * epsilon)))
                .toArray())
            .toArray(double[][]::new);
        // 按行计算概率密度函数
        IntStream.range(0, bound).forEach(input -> {
            double sum = Arrays.stream(cdfTable[input]).sum();
            IntStream.range(0, bound).forEach(output -> cdfTable[input][output] = cdfTable[input][output] / sum);
        });
        // 按行计算累计概率密度函数
        IntStream.range(0, bound).forEach(input -> {
            double cumulative = 0.0;
            for (int output = 0; output < bound; output++) {
                cumulative += cdfTable[input][output];
                cdfTable[input][output] = cumulative;
            }
        });
        // 因为求和结果可能有非常小的误差，因此要把最后一列统一设置为1.0
        IntStream.range(0, bound).forEach(input -> cdfTable[input][bound - 1] = 1.0);
    }

    @Override
    public CdpConfig getCdpConfig() {
        return boundIntegralCdpConfig;
    }

    @Override
    public double getEpsilon() {
        // 2 * Δf * ε
        return 2 * boundIntegralCdpConfig.getSensitivity() * boundIntegralCdpConfig.getBaseEpsilon();
    }

    @Override
    public double getDelta() {
        return 0.0;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        boundIntegralCdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public int randomize(int value) {
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        if (lowerBound == upperBound) {
            // 如果上下界相等，则不需要随机化，直接返回结果
            return value;
        }
        int normalizedValue = value - lowerBound;
        // 采样一个随机数，根据累计概率密度函数查看输出
        double u = boundIntegralCdpConfig.getRandom().nextDouble();
        for (int normalizedOutput = 0; normalizedOutput < bound; normalizedOutput++) {
            if (u <= cdfTable[normalizedValue][normalizedOutput]) {
                return normalizedOutput + lowerBound;
            }
        }
        // cdfTable的最后一列为1，而u < 1恒成立，因此理论上不可能输出到这里，保险起见这里输出最后一个值
        return upperBound;
    }
}
