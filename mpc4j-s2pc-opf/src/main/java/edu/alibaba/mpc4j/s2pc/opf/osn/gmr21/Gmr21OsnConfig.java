package edu.alibaba.mpc4j.s2pc.opf.osn.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory.OsnType;

/**
 * GMR21-OSN协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/10
 */
public class Gmr21OsnConfig extends AbstractMultiPartyPtoConfig implements OsnConfig {
    /**
     * COT协议配置项
     */
    private final CotConfig cotConfig;

    private Gmr21OsnConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public OsnType getPtoType() {
        return OsnType.GMR21;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21OsnConfig> {
        /**
         * 基础OT协议配置项
         */
        private CotConfig cotConfig;

        public Builder(boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Gmr21OsnConfig build() {
            return new Gmr21OsnConfig(this);
        }
    }
}
