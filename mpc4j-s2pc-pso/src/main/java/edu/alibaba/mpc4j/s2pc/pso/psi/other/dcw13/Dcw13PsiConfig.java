package edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

/**
 * DCW13-PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Dcw13PsiConfig extends AbstractMultiPartyPtoConfig implements FilterPsiConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * filter type
     */
    private final FilterType filterType;

    private Dcw13PsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.DCW13;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Dcw13PsiConfig> {
        /**
         * core COT config
         */
        private CoreCotConfig coreCotConfig;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            filterType = FilterType.SET_FILTER;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Dcw13PsiConfig build() {
            return new Dcw13PsiConfig(this);
        }
    }
}
