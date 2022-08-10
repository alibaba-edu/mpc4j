package edu.alibaba.mpc4j.dp.cdp.nominal;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base2指数CDP机制。此机制为指数CDP机制的高精度版本，来自于下述论文：
 * <p>
 * Christina Ilvento. Implementing the Exponential Mechanism with Base-2 Differential Privacy. CCS 2020, pp. 717–742.
 * </p>
 * 本实现参考了下述代码：
 * <p>
 * https://github.com/cilvento/b2_exponential_mechanism
 * </p>
 *
 * @author Xiaodong Zhang, Weiran Liu
 * @date 2022/4/23
 */
class Base2ExpCdp implements NominalCdp {
    /**
     * Base2指数CDP机制配置项
     */
    private Base2ExpCdpConfig base2ExpCdpConfig;
    /**
     * 计算精度
     */
    private MathContext mathContext;
    /**
     * 幂运算底数
     */
    private BigDecimal base;
    /**
     * 评分函数的归一化参数
     */
    private Map<String, BigDecimal> normalizeConstantMap;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof Base2ExpCdpConfig;
        base2ExpCdpConfig = (Base2ExpCdpConfig) cdpConfig;
        // 构建计算精度和幂运算底数
        mathContext = new MathContext(base2ExpCdpConfig.getPrecision());
        // base = 2^{-η}
        int etaX = base2ExpCdpConfig.getEtaX();
        int etaY = base2ExpCdpConfig.getEtaY();
        int etaZ = base2ExpCdpConfig.getEtaZ();
        base = new BigDecimal(etaX)
            .pow(etaZ, mathContext)
            .multiply(BigDecimal.valueOf(2).pow(-1 * etaY * etaZ, mathContext));
        // 计算所有标签的归一化值
        buildNormalizeConstant();
    }

    private void buildNormalizeConstant() {
        Set<String> nounSet = base2ExpCdpConfig.getNounSet();
        normalizeConstantMap = new HashMap<>(nounSet.size());
        for (String noun : nounSet) {
            BigDecimal constantValue = BigDecimal.ZERO;
            for (String targetNoun : nounSet) {
                constantValue = constantValue.add(getProbabilityFactor(noun, targetNoun));
            }
            normalizeConstantMap.put(noun, constantValue);
        }
    }

    private BigDecimal getProbabilityFactor(String noun1, String noun2) {
        if (noun1.equals(noun2)) {
            return BigDecimal.ONE;
        }
        // 返回base^{q}
        int utility = base2ExpCdpConfig.getUtilityMap().get(new NounPair(noun1, noun2)).intValue();

        return base.pow(utility, mathContext);
    }

    /**
     * 均匀采样一个值，该值满足（1）使用二进制表示一共 precision 比特位（2）取值在[0, 2^{startPow+1})内。
     *
     * @param startPow  开始采样的最高比特位。
     * @param precision 一共需要采样的比特位数。
     * @return 返回采样结果。
     */
    private BigDecimal getSampleValue(int startPow, int precision) {
        assert startPow < precision : "start power = " + startPow + ", must be less than precision = " + precision;
        BigDecimal s = BigDecimal.ZERO;
        int randomByteLength = CommonUtils.getByteLength(precision);
        byte[] randomBytes = new byte[randomByteLength];
        base2ExpCdpConfig.getRandom().nextBytes(randomBytes);
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

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        base2ExpCdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public CdpConfig getCdpConfig() {
        return base2ExpCdpConfig;
    }

    @Override
    public double getEpsilon() {
        // 对应于以e为底的差分隐私参数，ε = 2 * ln2 * η * Δq
        return 2 * Math.log(2) * base2ExpCdpConfig.getEta() * base2ExpCdpConfig.getDeltaQ();
    }

    @Override
    public double getDelta() {
        return 0;
    }

    @Override
    public String randomize(String noun) {
        assert base2ExpCdpConfig.getNounSet().contains(noun) : "The input noun is not in the NounSet: " + noun;
        // 生成一个[0, combinedSum)之间的随机数，位数为p位
        int precision = base2ExpCdpConfig.getPrecision();
        BigDecimal combinedSum = normalizeConstantMap.get(noun);
        int startPow = getStartPow(combinedSum);
        BigDecimal uniform = getSampleValue(startPow, precision);
        while (uniform.compareTo(combinedSum) >= 0) {
            uniform = getSampleValue(startPow, precision);
        }

        String lastValue = null;
        BigDecimal cumulativeSum = BigDecimal.ZERO;
        for (String targetValue : normalizeConstantMap.keySet()) {
            lastValue = targetValue;
            cumulativeSum = cumulativeSum.add(getProbabilityFactor(noun, targetValue));
            if (uniform.compareTo(cumulativeSum) <= 0) {
                return targetValue;
            }
        }
        return lastValue;
    }
}
