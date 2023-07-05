package edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.AidPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.AidPsiFactory;

/**
 * KMRS14 semi-honest aid PSI config.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public class Kmrs14ShAidPsiConfig extends AbstractMultiPartyPtoConfig implements AidPsiConfig {
    /**
     * coin-tossing config
     */
    private final CoinTossConfig coinTossConfig;

    private Kmrs14ShAidPsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coinTossConfig);
        coinTossConfig = builder.coinTossConfig;
    }

    @Override
    public AidPsiFactory.AidPsiType getPtoType() {
        return AidPsiFactory.AidPsiType.KMRS14_SH_AIDER;
    }

    public CoinTossConfig getCoinTossConfig() {
        return coinTossConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kmrs14ShAidPsiConfig> {
        /**
         * coin-tossing config
         */
        private CoinTossConfig coinTossConfig;

        public Builder() {
            coinTossConfig = CoinTossFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoinTossConfig(CoinTossConfig coinTossConfig) {
            this.coinTossConfig = coinTossConfig;
            return this;
        }

        @Override
        public Kmrs14ShAidPsiConfig build() {
            return new Kmrs14ShAidPsiConfig(this);
        }
    }
}
