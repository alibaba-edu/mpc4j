package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * RR16 PSI协议配置项。
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/06
 */
public class Rr16PsiConfig extends AbstractMultiPartyPtoConfig implements FilterPsiConfig {
    /**
     * LOT config
     */
    private final CoreCotConfig coreCotConfig;

    private final CoinTossConfig coinTossConfig;
    /**
     * filter type
     */
    private final FilterFactory.FilterType filterType;

    private Rr16PsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.coreCotConfig, builder.coinTossConfig);
        coreCotConfig = builder.coreCotConfig;
        coinTossConfig = builder.coinTossConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.RR16;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public CoinTossConfig getCoinTossConfig() {
        return coinTossConfig;
    }

    @Override
    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rr16PsiConfig> {
        /**
         * COT config
         */
        private final CoreCotConfig coreCotConfig;

        private final CoinTossConfig coinTossConfig;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            coinTossConfig = CoinTossFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            filterType = FilterFactory.FilterType.SET_FILTER;
        }

        public Builder setFilterType(FilterFactory.FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Rr16PsiConfig build() {
            return new Rr16PsiConfig(this);
        }
    }
}