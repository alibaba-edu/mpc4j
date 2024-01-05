package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.MpOprfPsiConfig;

/**
 * CM20-PSI config.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/10
 */
public class Cm20PsiConfig extends AbstractMultiPartyPtoConfig implements MpOprfPsiConfig {
    /**
     * MP-OPRF config
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * filter type
     */
    private final FilterType filterType;

    private Cm20PsiConfig(Cm20PsiConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mpOprfConfig);
        mpOprfConfig = builder.mpOprfConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.CM20;
    }

    @Override
    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cm20PsiConfig> {
        /**
         * MP-OPRF config
         */
        private final MpOprfConfig mpOprfConfig;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder() {
            mpOprfConfig = new Cm20MpOprfConfig.Builder().build();
            filterType = FilterType.SET_FILTER;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Cm20PsiConfig build() {
            return new Cm20PsiConfig(this);
        }
    }
}
