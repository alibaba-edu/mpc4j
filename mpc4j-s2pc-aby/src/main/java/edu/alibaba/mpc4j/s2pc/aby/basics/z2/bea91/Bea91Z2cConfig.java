package edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Bea91 Z2 circuit config.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91Z2cConfig extends AbstractMultiPartyPtoConfig implements Z2cConfig {
    /**
     * Z2 triple generation config
     */
    private final Z2TripleGenConfig z2TripleGenConfig;
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Bea91Z2cConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2TripleGenConfig);
        z2TripleGenConfig = builder.z2TripleGenConfig;
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public Z2TripleGenConfig getZ2TripleGenConfig() {
        return z2TripleGenConfig;
    }

    @Override
    public Z2cFactory.BcType getPtoType() {
        return Z2cFactory.BcType.BEA91;
    }

    @Override
    public int defaultRoundNum() {
        return z2TripleGenConfig.defaultRoundNum();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea91Z2cConfig> {
        /**
         * Z2 triple generation config
         */
        private final Z2TripleGenConfig z2TripleGenConfig;
        /**
         * no-choice COT config
         */
        private CotConfig cotConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            z2TripleGenConfig = Z2TripleGenFactory.createDefaultConfig(securityModel, silent);
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Bea91Z2cConfig build() {
            return new Bea91Z2cConfig(this);
        }
    }
}
