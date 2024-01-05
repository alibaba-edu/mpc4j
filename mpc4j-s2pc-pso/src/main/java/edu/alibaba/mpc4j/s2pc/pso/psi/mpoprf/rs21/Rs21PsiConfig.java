package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rs21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.MpOprfPsiConfig;

/**
 * RS21-PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Rs21PsiConfig extends AbstractMultiPartyPtoConfig implements MpOprfPsiConfig {
    /**
     * MP-OPRF config
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * filter type
     */
    private final FilterType filterType;

    private Rs21PsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.mpOprfConfig);
        mpOprfConfig = builder.mpOprfConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.RS21;
    }

    @Override
    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rs21PsiConfig> {
        /**
         * MP-OPRF config
         */
        private final MpOprfConfig mpOprfConfig;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder(SecurityModel securityModel) {
            mpOprfConfig = new Rs21MpOprfConfig.Builder(securityModel)
                .setOkvsType(Gf2kDokvsType.H2_BINARY_SINGLETON_GCT)
                .build();
            filterType = FilterType.SET_FILTER;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Rs21PsiConfig build() {
            return new Rs21PsiConfig(this);
        }
    }
}
