package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.config.BasicFoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;

import java.util.Set;

/**
 * Frequency Oracle Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class FoHhLdpConfig extends BaseHhLdpConfig {
    /**
     * Frequency Oracle LDP config
     */
    private final FoLdpConfig foLdpConfig;

    private FoHhLdpConfig(Builder builder) {
        super(builder);
        this.foLdpConfig = builder.foLdpConfig;
    }

    @Override
    public String getName() {
        return super.getName() + " (" + foLdpConfig.getName() + ")";
    }

    @Override
    public boolean isConverge() {
        return FoLdpFactory.isConverge(foLdpConfig.getType());
    }

    public FoLdpConfig getFoLdpConfig() {
        return foLdpConfig;
    }

    public static class Builder extends BaseHhLdpConfig.Builder {
        /**
         * Frequency Oracle LDP config
         */
        private final FoLdpConfig foLdpConfig;

        public Builder(Set<String> domainSet, int k, double windowEpsilon, int windowSize) {
            super(HhLdpFactory.HhLdpType.FO, domainSet, k, windowEpsilon, windowSize);
            foLdpConfig = new BasicFoLdpConfig
                .Builder(FoLdpFactory.FoLdpType.DE_INDEX, domainSet, windowEpsilon)
                .build();
        }

        public Builder(FoLdpConfig foLdpConfig, int k, int windowSize) {
            super(HhLdpFactory.HhLdpType.FO, foLdpConfig.getDomainSet(), k, foLdpConfig.getEpsilon(), windowSize);
            this.foLdpConfig = foLdpConfig;
        }

        @Override
        public FoHhLdpConfig build() {
            return new FoHhLdpConfig(this);
        }
    }
}
