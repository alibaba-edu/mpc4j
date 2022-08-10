package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.stream.IntStream;

/**
 * Base2指数有界整数CDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
class Base2ExpBoundIntegralCdp implements BoundIntegralCdp {
    /**
     * 配置项
     */
    private Base2ExpBoundIntegralCdpConfig boundIntegralCdpConfig;
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
     * 计算精度
     */
    private MathContext mathContext;
    /**
     * 幂运算底数
     */
    private BigDecimal base;
    /**
     * 为输入构建的累计概率因子二维数组，数组的长度和宽度均为bound
     */
    private BigDecimal[][] cumulateFactorTable;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof Base2ExpBoundIntegralCdpConfig;
        boundIntegralCdpConfig = (Base2ExpBoundIntegralCdpConfig) cdpConfig;
        lowerBound = boundIntegralCdpConfig.getLowerBound();
        upperBound = boundIntegralCdpConfig.getUpperBound();
        // 设置bound
        bound = upperBound - lowerBound + 1;
        // base = 2^{-η}
        mathContext = new MathContext(boundIntegralCdpConfig.getPrecision());
        int etaX = boundIntegralCdpConfig.getEtaX();
        int etaY = boundIntegralCdpConfig.getEtaY();
        int etaZ = boundIntegralCdpConfig.getEtaZ();
        base = new BigDecimal(etaX)
            .pow(etaZ, mathContext)
            .multiply(BigDecimal.valueOf(2).pow(-1 * etaY * etaZ, mathContext));
        // 构建概率因子二维数组
        cumulateFactorTable = IntStream.range(0, bound)
            .mapToObj(input -> IntStream.range(0, bound)
                .mapToObj(output -> getProbabilityFactor(Math.abs(input - output)))
                .toArray(BigDecimal[]::new))
            .toArray(BigDecimal[][]::new);
        // 按行计算累计概率因子二维数组
        IntStream.range(0, bound).forEach(input -> {
            BigDecimal cumulative = BigDecimal.ZERO;
            for (int output = 0; output < bound; output++) {
                cumulative = cumulative.add(cumulateFactorTable[input][output]);
                cumulateFactorTable[input][output] = cumulative;
            }
        });
    }

    private BigDecimal getProbabilityFactor(int distance) {
        if (distance == 0) {
            return BigDecimal.ONE;
        }
        return base.pow(distance, mathContext);
    }

    @Override
    public double getEpsilon() {
        // 对应于以e为底的差分隐私参数，ε = 2 * ln2 * η * Δf
        return 2 * Math.log(2) * boundIntegralCdpConfig.getEta() * boundIntegralCdpConfig.getSensitivity();
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
    public CdpConfig getCdpConfig() {
        return boundIntegralCdpConfig;
    }

    @Override
    public int randomize(int value) {
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        if (lowerBound == upperBound) {
            // 如果上下界相等，则不需要随机化，直接返回结果
            return value;
        }
        int normalizedInput = value - lowerBound;
        // 生成一个[0, combinedSum)之间的随机数，位数为p位
        int precision = boundIntegralCdpConfig.getPrecision();
        BigDecimal combinedSum = cumulateFactorTable[normalizedInput][bound - 1];
        int startPow = getStartPow(combinedSum);
        BigDecimal uniform = getSampleValue(startPow, precision);
        while (uniform.compareTo(combinedSum) >= 0) {
            uniform = getSampleValue(startPow, precision);
        }
        for (int normalizedTargetValue = 0; normalizedTargetValue < bound; normalizedTargetValue++) {
            if (uniform.compareTo(cumulateFactorTable[normalizedInput][normalizedTargetValue]) <= 0) {
                return normalizedTargetValue + lowerBound;
            }
        }
        return upperBound;
    }

    private BigDecimal getSampleValue(int startPow, int precision) {
        Preconditions.checkArgument(startPow < precision, "采样最高比特位不能超过精度可表示的最大值。");
        BigDecimal s = BigDecimal.ZERO;
        int randomByteLength = CommonUtils.getByteLength(precision);
        byte[] randomBytes = new byte[randomByteLength];
        boundIntegralCdpConfig.getRandom().nextBytes(randomBytes);
        BytesUtils.reduceByteArray(randomBytes, precision);
        boolean[] randomBinary = BinaryUtils.byteArrayToBinary(randomBytes, precision);
        for (int i = 0; i < precision; i++) {
            if (randomBinary[i]) {
                BigDecimal currBitValue = BigDecimal.valueOf(2).pow(startPow - i, mathContext);
                s = s.add(currBitValue);
            }
        }
        return s;
    }

    private int getStartPow(BigDecimal value) {
        // 当value = 0时，会出现死循环，因此需要单独判断
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return -1;
        }
        int startPow = 0;
        BigDecimal valueAbs = value.abs();
        while (BigDecimal.valueOf(2).pow(startPow, mathContext).compareTo(valueAbs) <= 0) {
            startPow++;
        }
        while (BigDecimal.valueOf(2).pow(startPow, mathContext).compareTo(valueAbs) > 0) {
            startPow--;
        }

        return startPow;
    }
}
