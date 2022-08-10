package edu.alibaba.mpc4j.dp.cdp.nominal;

import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 指数CDP机制。差分隐私最重要的枚举型机制，此机制来自于下述论文：
 * <p>
 * McSherry F, Talwar K. Mechanism design via differential privacy. STOC 2007, pp. 94-103.
 * </p>
 * 本实现参考了下述代码：
 * <p>
 * https://github.com/IBM/differential-privacy-library/blob/master/diffprivlib/mechanisms/exponential.py
 * </p>
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/23
 */
class ExpCdp implements NominalCdp {
    /**
     * 指数CDP机制配置项
     */
    private ExpCdpConfig expCdpConfig;
    /**
     * 评分函数的归一化参数
     */
    private Map<String, Double> normalizeConstantMap;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof ExpCdpConfig;
        expCdpConfig = (ExpCdpConfig) cdpConfig;
        // 预计算所有枚举值的累计概率密度函数
        buildNormalizeConstantMap();
    }

    private void buildNormalizeConstantMap() {
        Set<String> nounSet = expCdpConfig.getNounSet();
        normalizeConstantMap = new HashMap<>(nounSet.size());
        for (String noun : nounSet) {
            double constantValue = 0.0;
            for (String targetNoun : nounSet) {
                constantValue += getProbabilityFactor(noun, targetNoun);
            }
            normalizeConstantMap.put(noun, constantValue);
        }
    }

    private double getProbabilityFactor(String noun1, String noun2) {
        if (noun1.equals(noun2)) {
            // 相同枚举值的距离为0，概率因子为e^0 = 1
            return 1.0;
        }
        // 不同枚举值的概率因子为e^{-ε * q}
        double utility = expCdpConfig.getUtilityMap().get(new NounPair(noun1, noun2));
        return Math.exp(-1.0 * expCdpConfig.getBaseEpsilon() * utility);
    }

    @Override
    public double getEpsilon() {
        // 指数机制实现的是(2 * ε * Δq)-差分隐私性。
        return 2 * expCdpConfig.getBaseEpsilon() * expCdpConfig.getDeltaQ();
    }

    @Override
    public double getDelta() {
        return 0;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        expCdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public CdpConfig getCdpConfig() {
        return expCdpConfig;
    }

    @Override
    public String randomize(String noun) {
        assert expCdpConfig.getNounSet().contains(noun) : "The input noun is not in the NounSet: " + noun;
        double u = expCdpConfig.getRandom().nextDouble() * normalizeConstantMap.get(noun);
        double cumulativeProbability = 0.0;

        String lastNoun = null;
        for (String targetNoun : normalizeConstantMap.keySet()) {
            cumulativeProbability += getProbabilityFactor(noun, targetNoun);
            lastNoun = targetNoun;
            if (u <= cumulativeProbability) {
                return targetNoun;
            }
        }
        // 有可能因为精度问题导致已经到了最后一个value了，累计密度还没达到目标，这个时候直接回复最后一个value即可
        return lastNoun;
    }
}
