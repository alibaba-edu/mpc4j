package edu.alibaba.mpc4j.s2pc.aby.basics.zl64.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.Zl64cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.Zl64cFactory.Zl64cType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenFactory;

/**
 * Bea91 Zl circuit config.
 *
 * @author Li Peng
 * @date 2024/7/23
 */
public class Bea91Zl64cConfig extends AbstractMultiPartyPtoConfig implements Zl64cConfig {
    /**
     * Zl triple generation config
     */
    private final Zl64TripleGenConfig zl64TripleGenConfig;

    private Bea91Zl64cConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zl64TripleGenConfig);
        zl64TripleGenConfig = builder.zl64TripleGenConfig;
    }

    public Zl64TripleGenConfig getZl64TripleGenConfig() {
        return zl64TripleGenConfig;
    }

    @Override
    public Zl64cType getPtoType() {
        return Zl64cType.BEA91;
    }

    @Override
    public int defaultRoundNum(int l) {
        return zl64TripleGenConfig.defaultRoundNum(l);
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea91Zl64cConfig> {
        /**
         * Zl64 triple generation config
         */
        private final Zl64TripleGenConfig zl64TripleGenConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            zl64TripleGenConfig = Zl64TripleGenFactory.createDefaultConfig(securityModel, silent);
        }

        @Override
        public Bea91Zl64cConfig build() {
            return new Bea91Zl64cConfig(this);
        }
    }
}
