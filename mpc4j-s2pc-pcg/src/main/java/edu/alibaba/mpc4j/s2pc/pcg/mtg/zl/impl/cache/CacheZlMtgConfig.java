package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;

/**
 * cache Zl multiplication triple generator config.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class CacheZlMtgConfig extends AbstractMultiPartyPtoConfig implements ZlMtgConfig {
    /**
     * core multiplication triple generator config
     */
    private final ZlCoreMtgConfig coreMtgConfig;

    private CacheZlMtgConfig(Builder builder) {
        super(builder.coreMtgConfig);
        coreMtgConfig = builder.coreMtgConfig;
    }

    public ZlCoreMtgConfig getCoreMtgConfig() {
        return coreMtgConfig;
    }

    @Override
    public ZlMtgFactory.ZlMtgType getPtoType() {
        return ZlMtgFactory.ZlMtgType.CACHE;
    }

    @Override
    public Zl getZl() {
        return coreMtgConfig.getZl();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CacheZlMtgConfig> {
        /**
         * core multiplication triple generator config
         */
        private ZlCoreMtgConfig coreMtgConfig;

        public Builder(SecurityModel securityModel, Zl zl) {
            coreMtgConfig = ZlCoreMtgFactory.createDefaultConfig(securityModel, zl);
        }

        public Builder setCoreMtgConfig(ZlCoreMtgConfig coreMtgConfig) {
            this.coreMtgConfig = coreMtgConfig;
            return this;
        }

        @Override
        public CacheZlMtgConfig build() {
            return new CacheZlMtgConfig(this);
        }
    }
}
