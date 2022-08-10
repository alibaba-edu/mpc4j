package edu.alibaba.mpc4j.dp.cdp.nominal;

import java.security.SecureRandom;
import java.util.*;

/**
 * 指数CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
public class ExpCdpConfig implements NominalCdpConfig {
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 伪随机数生成器
     */
    private final Random random;
    /**
     * 所有可能的枚举值集合
     */
    private final Set<String> nounSet;
    /**
     * 评分函数
     */
    private final Map<NounPair, Double> utilityMap;
    /**
     * 评分最大变化量Δq
     */
    private final double deltaQ;

    private ExpCdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
        random = builder.random;
        nounSet = builder.nounSet;
        utilityMap = builder.utilityMap;
        deltaQ = builder.deltaQ;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    public Random getRandom() {
        return random;
    }

    @Override
    public Set<String> getNounSet() {
        return nounSet;
    }

    @Override
    public Map<NounPair, Double> getUtilityMap() {
        return utilityMap;
    }

    @Override
    public double getDeltaQ() {
        return deltaQ;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ExpCdpConfig> {
        /**
         * 基础差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 伪随机数生成器
         */
        private Random random;
        /**
         * 所有可能的枚举值集合
         */
        private Set<String> nounSet;
        /**
         * 评分函数
         */
        private Map<NounPair, Double> utilityMap;
        /**
         * 评分最大变化量Δq
         */
        private double deltaQ;
        /**
         * 所有枚举对距离
         */
        private final Collection<NounPairDistance> nounPairDistances;

        public Builder(double baseEpsilon, Collection<NounPairDistance> nounPairDistances) {
            assert baseEpsilon > 0 : "ε must be greater than 0";
            this.baseEpsilon = baseEpsilon;
            assert nounPairDistances != null;
            this.nounPairDistances = nounPairDistances;
            random = new SecureRandom();
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        @Override
        public ExpCdpConfig build() {
            // 解析nounPairDistances，设置枚举值集合、Δq和评分函数
            setUtility();
            assert nounSet.size() > 1 : "# of nouns must be greater than 1";
            assert utilityMap.size() > 0 : "utility function must contain at least one NounPair";
            return new ExpCdpConfig(this);
        }

        private void setUtility() {
            nounSet = new HashSet<>();
            utilityMap = new HashMap<>(nounPairDistances.size());
            deltaQ = 0.0;
            for (NounPairDistance nounPairDistance : nounPairDistances) {
                // 取最大的Δq
                deltaQ = Math.max(deltaQ, nounPairDistance.getDistance());
                NounPair nounPair = nounPairDistance.getNounPair();
                nounSet.add(nounPairDistance.getNounPair().getSmallNoun());
                nounSet.add(nounPairDistance.getNounPair().getLargeNoun());
                if (nounPair.getSmallNoun().equals(nounPair.getLargeNoun())) {
                    // 相同noun的距离默认为0，不需要处理
                    continue;
                }
                // NounPair已经保证了smallNoun <= largeNoun，utilityMap会保证不会插入重复的元素
                utilityMap.put(nounPair, nounPairDistance.getDistance());
            }
            checkUtilityFull();
        }

        private void checkUtilityFull() {
            for (String noun1 : nounSet) {
                for (String noun2 : nounSet) {
                    if (noun1.compareTo(noun2) >= 0) {
                        continue;
                    }
                    NounPair nounPair = new NounPair(noun1, noun2);
                    assert utilityMap.containsKey(nounPair) : "Utility value for ("
                        + nounPair.getSmallNoun() + ", " + nounPair.getLargeNoun()
                        + ") missing";
                }
            }
        }
    }
}
