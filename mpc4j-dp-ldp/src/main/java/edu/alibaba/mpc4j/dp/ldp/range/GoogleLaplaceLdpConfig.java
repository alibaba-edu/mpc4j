package edu.alibaba.mpc4j.dp.ldp.range;

/**
 * 谷歌拉普拉斯范围LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
public class GoogleLaplaceLdpConfig implements RangeLdpConfig {
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;

    private GoogleLaplaceLdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<GoogleLaplaceLdpConfig> {
        /**
         * 基础差分隐私参数ε
         */
        private final double baseEpsilon;

        public Builder(double baseEpsilon) {
            assert baseEpsilon > 0 : "ε must be greater than 0: " + baseEpsilon;
            this.baseEpsilon = baseEpsilon;
        }

        @Override
        public GoogleLaplaceLdpConfig build() {
            return new GoogleLaplaceLdpConfig(this);
        }
    }
}
