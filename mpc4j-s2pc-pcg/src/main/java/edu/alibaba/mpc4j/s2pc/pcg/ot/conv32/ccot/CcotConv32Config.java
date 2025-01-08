package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.ccot;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * F_3 -> F_2 modulus conversion using Core COT config.
 *
 * @author Weiran Liu
 * @date 2024/10/10
 */
public class CcotConv32Config extends AbstractMultiPartyPtoConfig implements Conv32Config {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;

    private CcotConv32Config(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public Conv32Type getPtoType() {
        return Conv32Type.CCOT;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CcotConv32Config> {
        /**
         * core COT config
         */
        private final CoreCotConfig coreCotConfig;

        public Builder(SecurityModel securityModel) {
            coreCotConfig = CoreCotFactory.createDefaultConfig(securityModel);
        }

        @Override
        public CcotConv32Config build() {
            return new CcotConv32Config(this);
        }
    }
}
