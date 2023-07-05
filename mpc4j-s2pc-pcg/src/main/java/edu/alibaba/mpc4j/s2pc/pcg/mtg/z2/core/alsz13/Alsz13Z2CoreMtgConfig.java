package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;

/**
 * ALSZ13-核布尔三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/6
 */
public class Alsz13Z2CoreMtgConfig extends AbstractMultiPartyPtoConfig implements Z2CoreMtgConfig {
    /**
     * NC-COT config
     */
    private final NcCotConfig ncCotConfig;

    private Alsz13Z2CoreMtgConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.ncCotConfig);
        ncCotConfig = builder.ncCotConfig;
    }

    public NcCotConfig getNcCotConfig() {
        return ncCotConfig;
    }

    @Override
    public Z2CoreMtgFactory.Z2CoreMtgType getPtoType() {
        return Z2CoreMtgFactory.Z2CoreMtgType.ALSZ13;
    }

    @Override
    public int maxNum() {
        return ncCotConfig.maxNum();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alsz13Z2CoreMtgConfig> {
        /**
         * NC-COT config
         */
        private NcCotConfig ncCotConfig;

        public Builder() {
            ncCotConfig = NcCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setNcCotConfig(NcCotConfig ncCotConfig) {
            this.ncCotConfig = ncCotConfig;
            return this;
        }

        @Override
        public Alsz13Z2CoreMtgConfig build() {
            return new Alsz13Z2CoreMtgConfig(this);
        }
    }
}
