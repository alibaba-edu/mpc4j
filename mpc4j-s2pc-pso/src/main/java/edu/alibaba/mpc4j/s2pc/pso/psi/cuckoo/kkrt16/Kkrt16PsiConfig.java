package edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16.Kkrt16OptOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * KKRT16-PSI config.
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Kkrt16PsiConfig extends AbstractMultiPartyPtoConfig implements FilterPsiConfig {
    /**
     * OPRF config
     */
    private final OprfConfig oprfConfig;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * filter type
     */
    private final FilterType filterType;

    private Kkrt16PsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.oprfConfig);
        oprfConfig = builder.oprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        filterType = builder.filterType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.KKRT16;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kkrt16PsiConfig> {
        /**
         * OPRF config
         */
        private final OprfConfig oprfConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder() {
            oprfConfig = new Kkrt16OptOprfConfig.Builder().build();
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
            filterType = FilterType.SET_FILTER;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Kkrt16PsiConfig build() {
            return new Kkrt16PsiConfig(this);
        }
    }
}
