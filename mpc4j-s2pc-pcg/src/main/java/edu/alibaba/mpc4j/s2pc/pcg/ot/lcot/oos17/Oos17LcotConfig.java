package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.oos17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;

/**
 * OOS17-2^l选1-COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/8
 */
public class Oos17LcotConfig extends AbstractMultiPartyPtoConfig implements LcotConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;

    private Oos17LcotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public LcotFactory.LcotType getPtoType() {
        return LcotFactory.LcotType.OOS17;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Oos17LcotConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Oos17LcotConfig build() {
            return new Oos17LcotConfig(this);
        }
    }
}
