package edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * RT21-PSI config.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/8/10
 */
public class Rt21PsiConfig extends AbstractMultiPartyPtoConfig implements FilterPsiConfig {
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * filter type
     */
    private final FilterType filterType;

    private Rt21PsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        filterType = builder.filterType;
        okvsType = builder.okvsType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.RT21;
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public Gf2eDokvsType getOkvsType() {
        return okvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rt21PsiConfig> {
        /**
         * filter type
         */
        private FilterType filterType;
        /**
         * OKVS type
         */
        private Gf2eDokvsType okvsType;

        public Builder() {
            filterType = FilterType.SET_FILTER;
            okvsType = Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        public Builder setOkvsType(Gf2eDokvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        @Override
        public Rt21PsiConfig build() {
            return new Rt21PsiConfig(this);
        }
    }
}