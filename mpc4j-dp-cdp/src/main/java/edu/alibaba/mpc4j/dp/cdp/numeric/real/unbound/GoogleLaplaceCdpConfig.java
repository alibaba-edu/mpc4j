package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

/**
 * 谷歌拉普拉斯机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/20
 */
public class GoogleLaplaceCdpConfig implements UnboundRealCdpConfig {
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 敏感度Δf
     */
    private final double sensitivity;

    private GoogleLaplaceCdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
        sensitivity = builder.sensitivity;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    @Override
    public double getSensitivity() {
        return sensitivity;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<GoogleLaplaceCdpConfig> {
        /**
         * 基础差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 敏感度Δf
         */
        private final double sensitivity;

        public Builder(double baseEpsilon, double sensitivity) {
            assert baseEpsilon > 0 : "ε must be greater or equal than 0: " + baseEpsilon;
            this.baseEpsilon = baseEpsilon;
            assert sensitivity > 0 : "Δf must be greater than 0: " + sensitivity;
            this.sensitivity = sensitivity;
        }

        @Override
        public GoogleLaplaceCdpConfig build() {
            return new GoogleLaplaceCdpConfig(this);
        }
    }
}
