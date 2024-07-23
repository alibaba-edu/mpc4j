package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfFactory.F32SowOprfType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * APRP24 (F3, F2)-sowOPRF config.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class Aprr24F32SowOprfConfig extends AbstractMultiPartyPtoConfig implements F32SowOprfConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * F_3 -> F_2 modulus conversion config
     */
    private final Conv32Config conv32Config;

    private Aprr24F32SowOprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig, builder.conv32Config);
        coreCotConfig = builder.coreCotConfig;
        conv32Config = builder.conv32Config;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public Conv32Config getConv32Config() {
        return conv32Config;
    }

    @Override
    public F32SowOprfType getPtoType() {
        return F32SowOprfType.APRR24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aprr24F32SowOprfConfig> {
        /**
         * core COT config
         */
        private final CoreCotConfig coreCotConfig;
        /**
         * F_3 -> F_2 modulus conversion config
         */
        private final Conv32Config conv32Config;

        public Builder(Conv32Type conv32Type) {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            conv32Config = Conv32Factory.createDefaultConfig(SecurityModel.SEMI_HONEST, conv32Type);
        }

        @Override
        public Aprr24F32SowOprfConfig build() {
            return new Aprr24F32SowOprfConfig(this);
        }
    }
}
