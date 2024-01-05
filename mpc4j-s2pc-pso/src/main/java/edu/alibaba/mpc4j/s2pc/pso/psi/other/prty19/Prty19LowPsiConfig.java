package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

/**
 * PRTY19-PSI (low communication) protocol config.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/17
 */
public class Prty19LowPsiConfig extends AbstractMultiPartyPtoConfig implements FilterPsiConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * filter type
     */
    private final FilterType filterType;

    private Prty19LowPsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
        okvsType = builder.okvsType;
        filterType = builder.filterType;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.PRTY19_LOW;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public Gf2eDokvsType getOkvsType() {
        return okvsType;
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Prty19LowPsiConfig> {
        /**
         * core COT config
         */
        private CoreCotConfig coreCotConfig;
        /**
         * OKVS type
         */
        private Gf2eDokvsType okvsType;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            okvsType = Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
            filterType = FilterType.SET_FILTER;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setOkvsType(Gf2eDokvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Prty19LowPsiConfig build() {
            return new Prty19LowPsiConfig(this);
        }
    }
}