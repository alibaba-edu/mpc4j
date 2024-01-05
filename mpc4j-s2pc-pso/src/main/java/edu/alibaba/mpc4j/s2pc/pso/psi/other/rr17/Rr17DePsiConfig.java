package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * RR17 Dual Execution PSI config.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17DePsiConfig extends AbstractMultiPartyPtoConfig implements FilterPsiConfig {
    /**
     * LOT configure
     */
    private final LcotConfig lcotConfig;

    /**
     * CoinToss configure
     */
    private final CoinTossConfig coinTossConfig;

    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * The parameter decide the number of PhaseHash
     */
    private final int divParam4PhaseHash;

    private Rr17DePsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.lcotConfig, builder.coinTossConfig);
        lcotConfig = builder.lcotConfig;
        coinTossConfig = builder.coinTossConfig;
        filterType = builder.filterType;
        divParam4PhaseHash = builder.divParam4PhaseHash;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.RR17_DE;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    public CoinTossConfig getCoinTossConfig() {
        return coinTossConfig;
    }

    @Override
    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public int getDivParam4PhaseHash() {
        return divParam4PhaseHash;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rr17DePsiConfig> {
        /**
         * LOT type
         */
        private final LcotConfig lcotConfig;
        /**
         * CoinToss configure
         */
        private final CoinTossConfig coinTossConfig;
        /**
         * filter type
         */
        private FilterFactory.FilterType filterType;
        /**
         * The parameter decide the number of PhaseHash, the paper uses 4 or 10
         */
        private int divParam4PhaseHash;

        public Builder() {
            lcotConfig = LcotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            coinTossConfig = CoinTossFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            filterType = FilterFactory.FilterType.SET_FILTER;
            divParam4PhaseHash = 4;
        }

        public Builder setFilterType(FilterFactory.FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        public Builder setDivParam(int divParam4PhaseHash) {
            // 4 for LAN setting, 10 for WAN setting
            this.divParam4PhaseHash = divParam4PhaseHash;
            return this;
        }

        @Override
        public Rr17DePsiConfig build() {
            return new Rr17DePsiConfig(this);
        }
    }
}
