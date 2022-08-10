package edu.alibaba.mpc4j.dp.cdp.nominal;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

import java.security.SecureRandom;
import java.util.*;

/**
 * Base2指数CDP机制配置项。
 *
 * @author Xiaodong Zhang
 * @date 2022/4/23
 */
public class Base2ExpCdpConfig implements NominalCdpConfig {
    /**
     * 隐私参数η_x
     */
    private final int etaX;
    /**
     * 差分隐私参数η_y
     */
    private final int etaY;
    /**
     * 差分隐私参数η_z
     */
    private final int etaZ;
    /**
     * 采样数据精度
     */
    private final int precision;
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

    private Base2ExpCdpConfig(Builder builder) {
        this.etaX = builder.etaX;
        this.etaY = builder.etaY;
        this.etaZ = builder.etaZ;
        this.precision = builder.precision;
        this.random = builder.random;
        this.nounSet = builder.nounSet;
        this.utilityMap = builder.utilityMap;
        this.deltaQ = builder.deltaQ;
    }

    public int getEtaX() {
        return etaX;
    }

    public int getEtaY() {
        return etaY;
    }

    public int getEtaZ() {
        return etaZ;
    }

    /**
     * 返回η = -z * log(x / 2^y)。
     *
     * @return η。
     */
    public double getEta() {
        return (-1) * etaZ * DoubleUtils.log2(etaX / Math.pow(2, etaY));
    }

    public int getPrecision() {
        return precision;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Base2ExpCdpConfig> {
        /**
         * 差分隐私参数η_x
         */
        private final int etaX;
        /**
         * 差分隐私参数η_y
         */
        private final int etaY;
        /**
         * 差分隐私参数η_z
         */
        private int etaZ;
        /**
         * 采样数据精度
         */
        private int precision;
        /**
         * 伪随机数生成器
         */
        private Random random;
        /**
         * 评分函数伪随机数生成器
         */
        private Random utilityRandom;
        /**
         * 指数机制的输入域
         */
        private Set<String> nounSet;
        /**
         * 指数机制的评分函数
         */
        private Map<NounPair, Double> utilityMap;
        /**
         * 评分最大变化量Δq
         */
        private double deltaQ;
        /**
         * 记录最小距离值。最大距离值可用Δq表示
         */
        private double minQ;
        /**
         * 所有枚举对距离
         */
        private final Collection<NounPairDistance> nounPairDistances;

        public Builder(int etaX, int etaY, List<NounPairDistance> nounPairDistances) {
            assert etaX > 0 : "η_x must be greater than 0";
            assert etaY > 0 : "η_y must be greater than 0";
            assert etaX <= Math.pow(2, etaY) : "η_x / 2^(η_y) must be less or equal than 1";
            this.etaX = etaX;
            this.etaY = etaY;
            etaZ = 1;
            assert nounPairDistances != null;
            this.nounPairDistances = nounPairDistances;
            random = new SecureRandom();
            utilityRandom = new SecureRandom();
        }

        public Builder setEtaZ(int etaZ) {
            assert etaZ > 0 : "η_z must be greater than 0";
            this.etaZ = etaZ;
            return this;
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        public Builder setUtilityRandom(Random utilityRandom) {
            this.utilityRandom = utilityRandom;
            return this;
        }

        public Builder setPrecision(int precision) {
            assert precision > 0 : "precision must be greater than 0";
            this.precision = precision;
            return this;
        }

        @Override
        public Base2ExpCdpConfig build() {
            // 解析utilityDistances，设置枚举值集合、Δq和评分函数
            setUtility();
            assert nounSet.size() > 1 : "# of nouns must be greater than 1";
            assert utilityMap.size() > 0 : "utility function must contain at least one NounPair";
            // 检查精度
            int maxOutput = nounSet.size();
            int bx = Math.max((int) Math.ceil(DoubleUtils.log2(etaX)), 1);
            int theoreticalPrecision =
                (int) ((Math.max(1, Math.abs(deltaQ)) + Math.max(1, Math.abs(minQ))) *
                    (etaZ * (etaY + bx)) + maxOutput);
            if (precision == 0) {
                // 如果未设置精度，则计算得到默认精度
                precision = theoreticalPrecision;
            } else {
                assert precision >= theoreticalPrecision : "Cannot achieve precision: " + precision
                    + ", because the maximum theoretical precision is: " + theoreticalPrecision;
            }

            return new Base2ExpCdpConfig(this);
        }

        private void setUtility() {
            nounSet = new HashSet<>();
            utilityMap = new HashMap<>(nounPairDistances.size());

            for (NounPairDistance nounPairDistance : nounPairDistances) {
                // 设置评分函数前，需要将评分函数随机取整
                double distance = nounPairDistance.getDistance();
                // 计算向下取整的概率
                double floorProbability = Math.ceil(distance) - distance;
                double u = utilityRandom.nextDouble();
                double roundDistance = u > floorProbability ? Math.ceil(distance) : Math.floor(distance);
                deltaQ = Math.max(deltaQ, roundDistance);
                minQ = Math.min(minQ, roundDistance);
                // 设置输入域
                NounPair nounPair = nounPairDistance.getNounPair();
                nounSet.add(nounPair.getSmallNoun());
                nounSet.add(nounPair.getLargeNoun());
                if (nounPair.getSmallNoun().equals(nounPair.getLargeNoun())) {
                    // 相同noun的距离默认为0，不需要处理
                    continue;
                }
                // NounPair已经保证了smallNoun <= largeNoun，utilityMap会保证不会插入重复的元素
                utilityMap.put(nounPair, roundDistance);
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
