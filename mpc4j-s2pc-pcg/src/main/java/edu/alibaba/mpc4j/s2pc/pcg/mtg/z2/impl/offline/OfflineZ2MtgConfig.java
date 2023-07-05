package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;

/**
 * offline Z2 multiplication triple generator config.
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
public class OfflineZ2MtgConfig extends AbstractMultiPartyPtoConfig implements Z2MtgConfig {
    /**
     * core multiplication triple generator config
     */
    private final Z2CoreMtgConfig coreMtgConfig;

    private OfflineZ2MtgConfig(Builder builder) {
        super(builder.coreMtgConfig);
        coreMtgConfig = builder.coreMtgConfig;
    }

    public Z2CoreMtgConfig getCoreMtgConfig() {
        return coreMtgConfig;
    }

    @Override
    public Z2MtgFactory.Z2MtgType getPtoType() {
        return Z2MtgFactory.Z2MtgType.OFFLINE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OfflineZ2MtgConfig> {
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
        public OfflineZ2MtgConfig build() {
            return new OfflineZ2MtgConfig(this);
        }
    }
}