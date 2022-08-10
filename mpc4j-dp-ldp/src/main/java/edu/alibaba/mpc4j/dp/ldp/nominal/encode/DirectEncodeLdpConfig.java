package edu.alibaba.mpc4j.dp.ldp.nominal.encode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 直接编码LDP机制配置项。
 *
 * @author Xiaodong Zhang, Weiran Liu
 * @date 2022/4/28
 */
public class DirectEncodeLdpConfig implements EncodeLdpConfig {
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 标签枚举值列表
     */
    private final ArrayList<String> labelArrayList;
    /**
     * 标签集合
     */
    private final Set<String> labelSet;
    /**
     * 伪随机数生成器
     */
    private final Random random;

    private DirectEncodeLdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
        labelArrayList = builder.labelArrayList;
        // 执行枚举LDP机制时要验证输入是否在标签集合内。为了提高效率，不能每次都新创建一个集合，而是要预先创建好
        labelSet = new HashSet<>(labelArrayList);
        random = builder.random;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    @Override
    public ArrayList<String> getLabelArrayList() {
        return labelArrayList;
    }

    @Override
    public Set<String> getLabelSet() {
        return labelSet;
    }

    public Random getRandom() {
        return random;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectEncodeLdpConfig> {
        /**
         * 基础差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 标签枚举值列表
         */
        private final ArrayList<String> labelArrayList;
        /**
         * 伪随机数生成器
         */
        private Random random;

        public Builder(double baseEpsilon, List<String> labelArrayList) {
            assert baseEpsilon > 0 : "ε must be greater than 0";
            this.baseEpsilon = baseEpsilon;
            // 设置标签列表、标签集合和标签索引值映射
            this.labelArrayList = labelArrayList.stream()
                // 去重
                .distinct()
                // 排序
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));
            assert this.labelArrayList.size() > 1 : "|D| must be greater than 1";
            random = new Random();
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        @Override
        public DirectEncodeLdpConfig build() {
            return new DirectEncodeLdpConfig(this);
        }
    }
}
