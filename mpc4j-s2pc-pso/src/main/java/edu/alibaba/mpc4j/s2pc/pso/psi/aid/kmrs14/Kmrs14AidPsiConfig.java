package edu.alibaba.mpc4j.s2pc.pso.psi.aid.kmrs14;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import edu.alibaba.mpc4j.s2pc.pso.psi.aid.AidPsiConfig;

/**
 * KMRS14 semi-honest aid PSI config.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public class Kmrs14AidPsiConfig extends AbstractMultiPartyPtoConfig implements AidPsiConfig {
    /**
     * coin-tossing config
     */
    private final CoinTossConfig coinTossConfig;

    private Kmrs14AidPsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coinTossConfig);
        coinTossConfig = builder.coinTossConfig;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.AID_KMRS14;
    }

    public CoinTossConfig getCoinTossConfig() {
        return coinTossConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kmrs14AidPsiConfig> {
        /**
         * coin-tossing config
         */
        private final CoinTossConfig coinTossConfig;

        public Builder() {
            coinTossConfig = CoinTossFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        @Override
        public Kmrs14AidPsiConfig build() {
            return new Kmrs14AidPsiConfig(this);
        }
    }
}
