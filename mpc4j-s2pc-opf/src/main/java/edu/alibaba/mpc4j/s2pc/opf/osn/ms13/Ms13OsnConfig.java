package edu.alibaba.mpc4j.s2pc.opf.osn.ms13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;

/**
 * MS13-OSN协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public class Ms13OsnConfig extends AbstractMultiPartyPtoConfig implements OsnConfig {
    /**
     * COT协议配置项
     */
    private final CotConfig cotConfig;

    private Ms13OsnConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public OsnFactory.OsnType getPtoType() {
        return OsnFactory.OsnType.MS13;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ms13OsnConfig> {
        /**
         * COT协议配置项
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
        public Ms13OsnConfig build() {
            return new Ms13OsnConfig(this);
        }
    }
}
