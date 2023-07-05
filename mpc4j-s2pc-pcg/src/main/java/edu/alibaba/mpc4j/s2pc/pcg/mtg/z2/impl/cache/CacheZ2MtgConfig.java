package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;

/**
 * cache Z2 multiplication triple generator config.
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class CacheZ2MtgConfig extends AbstractMultiPartyPtoConfig implements Z2MtgConfig {
    /**
     * core multiplication triple generator config
     */
    private final Z2CoreMtgConfig coreMtgConfig;

    private CacheZ2MtgConfig(Builder builder) {
        super(builder.coreMtgConfig);
        coreMtgConfig = builder.coreMtgConfig;
    }

    public Z2CoreMtgConfig getCoreMtgConfig() {
        return coreMtgConfig;
    }

    @Override
    public Z2MtgFactory.Z2MtgType getPtoType() {
        return Z2MtgFactory.Z2MtgType.CACHE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CacheZ2MtgConfig> {
        /**
         * core multiplication triple generator config
         */
        private Z2CoreMtgConfig coreMtgConfig;

        public Builder(SecurityModel securityModel) {
            coreMtgConfig = Z2CoreMtgFactory.createDefaultConfig(securityModel, true);
        }

        public Builder setCoreMtgConfig(Z2CoreMtgConfig coreMtgConfig) {
            this.coreMtgConfig = coreMtgConfig;
            return this;
        }

        @Override
        public CacheZ2MtgConfig build() {
            return new CacheZ2MtgConfig(this);
        }
    }
}
