package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * RR17 Dual Execution PSI config.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17EcPsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * LOT configure
     */
    private final LcotConfig lcotConfig;

    /**
     * CoinTossing configure
     */
    private final CoinTossConfig coinTossConfig;
    /**
     * This parameter decide the number of PhaseHash, the paper uses 4 or 10
     */
    private final int divParam4PhaseHash;


    private Rr17EcPsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.lcotConfig, builder.coinTossConfig);
        lcotConfig = builder.lcotConfig;
        coinTossConfig = builder.coinTossConfig;
        divParam4PhaseHash = builder.divParam4PhaseHash;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.RR17_EC;
    }


    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    public CoinTossConfig getCoinTossConfig() {
        return coinTossConfig;
    }

    public int getDivParam4PhaseHash() {
        return divParam4PhaseHash;
    }


    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rr17EcPsiConfig> {
        /**
         * LOT configure
         */
        private final LcotConfig lcotConfig;
        /**
         * CoinToss configure
         */
        private final CoinTossConfig coinTossConfig;
        /**
         * This parameter decide the number of PhaseHash, the paper uses 4 or 10
         */
        private int divParam4PhaseHash;

        public Builder() {
            lcotConfig = LcotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            coinTossConfig = CoinTossFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            divParam4PhaseHash = 4;
        }

        public Builder setDivParam(int divParam4PhaseHash) {
            // 4 in LAN setting, 10 in WAN setting
            this.divParam4PhaseHash = divParam4PhaseHash;
            return this;
        }

        @Override
        public Rr17EcPsiConfig build() {
            return new Rr17EcPsiConfig(this);
        }
    }
}