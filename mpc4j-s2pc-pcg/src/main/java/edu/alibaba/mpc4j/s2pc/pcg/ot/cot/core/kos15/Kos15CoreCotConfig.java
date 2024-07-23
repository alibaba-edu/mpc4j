package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * KOS15-核COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
public class Kos15CoreCotConfig extends AbstractMultiPartyPtoConfig implements CoreCotConfig {
    /**
     * base OT
     */
    private final BaseOtConfig baseOtConfig;

    private Kos15CoreCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.baseOtConfig);
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public CoreCotFactory.CoreCotType getPtoType() {
        return CoreCotFactory.CoreCotType.KOS15;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kos15CoreCotConfig> {
        /**
         * base OT
         */
        private final BaseOtConfig baseOtConfig;

        public Builder() {
            baseOtConfig = BaseOtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public Kos15CoreCotConfig build() {
            return new Kos15CoreCotConfig(this);
        }
    }
}
