package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * YWL20-BSP-COT半诚实安全协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20ShBspCotConfig extends AbstractMultiPartyPtoConfig implements BspCotConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * DPPRF协议配置项
     */
    private final BpDpprfConfig bpDpprfConfig;

    private Ywl20ShBspCotConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig, builder.bpDpprfConfig);
        coreCotConfig = builder.coreCotConfig;
        bpDpprfConfig = builder.bpDpprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public BpDpprfConfig getBpDpprfConfig() {
        return bpDpprfConfig;
    }

    @Override
    public BspCotFactory.BspCotType getPtoType() {
        return BspCotFactory.BspCotType.YWL20_SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20ShBspCotConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;
        /**
         * DPPRF协议配置项
         */
        private BpDpprfConfig bpDpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            bpDpprfConfig = BpDpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setBpDpprfConfig(BpDpprfConfig bpDpprfConfig) {
            this.bpDpprfConfig = bpDpprfConfig;
            return this;
        }

        @Override
        public Ywl20ShBspCotConfig build() {
            return new Ywl20ShBspCotConfig(this);
        }
    }
}
