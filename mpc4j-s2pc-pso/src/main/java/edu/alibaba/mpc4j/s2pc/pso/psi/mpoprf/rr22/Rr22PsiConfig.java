package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.MpOprfPsiConfig;

/**
 * RR22-PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Rr22PsiConfig extends AbstractMultiPartyPtoConfig implements MpOprfPsiConfig {
    /**
     * MP-OPRF config
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * filter type
     */
    private final FilterType filterType;

    private Rr22PsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.mpOprfConfig);
        mpOprfConfig = builder.mpOprfConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.RR22;
    }

    @Override
    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rr22PsiConfig> {
        /**
         * MP-OPRF config
         */
        private final MpOprfConfig mpOprfConfig;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder(SecurityModel securityModel) {
            this(securityModel, Gf2kDokvsType.H3_CLUSTER_FIELD_BLAZE_GCT);
        }

        public Builder(SecurityModel securityModel, Gf2kDokvsType okvsType) {
            mpOprfConfig = new Rs21MpOprfConfig.Builder(securityModel)
                .setOkvsType(okvsType)
                .build();
            filterType = FilterType.SET_FILTER;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Rr22PsiConfig build() {
            return new Rr22PsiConfig(this);
        }
    }
}
